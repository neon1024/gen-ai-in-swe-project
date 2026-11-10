package com.ubb.genai.events.authoring.application;

import static org.junit.jupiter.api.Assertions.*;

import com.ubb.genai.events.authoring.domain.EventCategory;
import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector="
    + "com.ubb.genai.events.authoring.application.AggregateReadConsistencyTests$ReadBarrier")
@Import({PostgresTestConfiguration.class, ActivityAuthoringTests.Ports.class})
public class AggregateReadConsistencyTests {
  @Autowired EventAuthoringService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
  @Autowired com.ubb.genai.events.authoring.ports.PublishedEventsReadContract published;

  public static class ReadBarrier implements StatementInspector {
    static volatile CountDownLatch rootLoaded;
    static volatile CountDownLatch resumeChildren;

    @Override
    public String inspect(String sql) {
      if (Thread.currentThread().getName().equals("authoring-snapshot-reader")
          && sql.startsWith("select") && sql.contains("event_authoring.activity")) {
        rootLoaded.countDown();
        try {
          if (!resumeChildren.await(15, TimeUnit.SECONDS))
            throw new IllegalStateException("Timed out waiting for the concurrent writer");
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("Reader interrupted", exception);
        }
      }
      return sql;
    }
  }

  @BeforeEach
  void reset() {
    ReadBarrier.rootLoaded = new CountDownLatch(1);
    ReadBarrier.resumeChildren = new CountDownLatch(1);
    jdbc.update("delete from event_authoring.agenda_item");
    jdbc.update("delete from event_authoring.activity");
    jdbc.update("delete from event_authoring.event");
  }

  EventAuthoringService.ActivityCommand activity(long version, String title) {
    var start = OffsetDateTime.parse("2026-12-01T10:00:00+02:00");
    return new EventAuthoringService.ActivityCommand(title, "Description", start,
        start.plusHours(2), "Room", null, 5, null, null, "Public", version);
  }

  @Test
  void publishedProjectionKeepsRootAndChildrenInOneCommittedSnapshot() throws Exception {
    var event = service.createDraft(new EventAuthoringService.CreateEventCommand(
        "Event", "Details", EventCategory.OTHER, "org"));
    service.addActivity(event.id(), activity(0, "Original session"));
    service.publish(event.id(), 1);
    var before = published.publishedEvents();
    try (var reader = Executors.newSingleThreadExecutor(task ->
        new Thread(task, "authoring-snapshot-reader"))) {
      var read = reader.submit(() -> published.publishedEvents());
      try {
        assertTrue(ReadBarrier.rootLoaded.await(10, TimeUnit.SECONDS));
        // Exercise committed storage concurrency independently of deferred Published commands.
        new org.springframework.transaction.support.TransactionTemplate(transactions)
            .executeWithoutResult(status -> {
              assertEquals(1, jdbc.update("update event_authoring.event set version=version+1 where id=? and version=2", event.id()));
              assertEquals(1, jdbc.update("update event_authoring.activity set title=? where event_id=?", "Updated session", event.id()));
            });
      } finally {
        ReadBarrier.resumeChildren.countDown();
      }
      assertEquals(before, read.get(10, TimeUnit.SECONDS));
      var latest = published.publishedEvents().getFirst();
      assertEquals(3, latest.version());
      assertEquals(1, latest.activities().size());
      assertEquals("Updated session", latest.activities().getFirst().title());
    }
  }

  @Test
  void aggregateReadKeepsRootVersionAndChildrenInOneSnapshotDuringConcurrentCommit()
      throws Exception {
    var event = service.createDraft(new EventAuthoringService.CreateEventCommand(
        "Event", "Details", EventCategory.OTHER, "org"));
    assertTrue(AopUtils.isAopProxy(service), "Service calls must use Spring transaction interception");
    var added = service.addActivity(event.id(), activity(0, "Original session"));
    var activityId = added.activities().getFirst().id();
    var time = added.activities().getFirst().start();
    var before = service.addAgenda(event.id(), activityId,
        new EventAuthoringService.AgendaCommand(time, "Original talk", "Original speaker", 1L));
    var itemId = before.activities().getFirst().agenda().getFirst().id();
    try (var reader = Executors.newSingleThreadExecutor(task ->
        new Thread(task, "authoring-snapshot-reader"))) {
      var read = reader.submit(() -> service.getEvent(event.id()));
      try {
        assertTrue(ReadBarrier.rootLoaded.await(10, TimeUnit.SECONDS),
            "The reader must load the root before the writer runs");
        var changed = service.updateActivity(event.id(), activityId, activity(2, "Updated session"));
        assertEquals(3, changed.version());
        var after = service.updateAgenda(event.id(), activityId, itemId,
            new EventAuthoringService.AgendaCommand(time, "Updated talk", "Updated speaker", 3L));
        assertEquals(4, after.version());
        assertEquals("Updated session", after.activities().getFirst().title());
        assertEquals("Updated talk", after.activities().getFirst().agenda().getFirst().title());
        assertEquals(4L, jdbc.queryForObject(
            "select version from event_authoring.event where id=?", Long.class, event.id()),
            "The writer must commit before the reader resumes its child SELECT");
      } finally {
        ReadBarrier.resumeChildren.countDown();
      }
      var observed = read.get(10, TimeUnit.SECONDS);
      assertEquals(before, observed,
          "A GET started before the concurrent commit must retain a coherent aggregate snapshot");
      var latest = service.getEvent(event.id());
      assertEquals(4, latest.version());
      assertEquals("Updated session", latest.activities().getFirst().title());
      assertEquals("Updated talk", latest.activities().getFirst().agenda().getFirst().title());
      assertEquals("Updated speaker", latest.activities().getFirst().agenda().getFirst().speaker());
    }
  }
}
