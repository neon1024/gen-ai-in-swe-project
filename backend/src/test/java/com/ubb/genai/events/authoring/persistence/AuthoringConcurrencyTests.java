package com.ubb.genai.events.authoring.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.ubb.genai.events.authoring.application.*;
import com.ubb.genai.events.authoring.domain.*;
import com.ubb.genai.events.authoring.ports.*;
import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import({PostgresTestConfiguration.class, AuthoringConcurrencyTests.Ports.class})
class AuthoringConcurrencyTests {
  @TestConfiguration
  static class Ports {
    @Bean
    AuthoringIdentityPort identity() {
      return () -> new AuthoringIdentityPort.Actor("owner", true);
    }

    @Bean
    OrganizationAccessPort organizations() {
      return (s, o) -> OrganizationAccessPort.Role.ORGANIZER;
    }

    @Bean
    EventTeamAccessPort team() {
      return (s, e) -> EventTeamAccessPort.Role.NONE;
    }

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired EventAuthoringService service;
  @Autowired PlatformTransactionManager transactions;
  @Autowired JdbcTemplate jdbc;
  @Autowired AuthoringEventRepository repository;
  @Autowired PublishedEventsReadContract published;

  @BeforeEach
  void reset() {
    jdbc.update("delete from event_authoring.agenda_item");
    jdbc.update("delete from event_authoring.activity");
    jdbc.update("delete from event_authoring.event");
  }

  UUID ready() {
    var e =
        service.createDraft(
            new EventAuthoringService.CreateEventCommand(
                "Event", "Details", EventCategory.WORKSHOP, "org"));
    service.addActivity(
        e.id(),
        new EventAuthoringService.ActivityCommand(
            "Session",
            "Details",
            OffsetDateTime.parse("2026-12-01T10:00:00+02:00"),
            OffsetDateTime.parse("2026-12-01T12:00:00+02:00"),
            "Room",
            null,
            10,
            null,
            null,
            null,
            0L));
    return e.id();
  }

  @Test
  void US17_projectionOnlyShowsCommittedPublishedAggregates() throws Exception {
    UUID draft = ready();
    UUID id = ready();
    assertTrue(published.publishedEvents().isEmpty());
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              service.publish(id, 1);
              assertTrue(
                  published.publishedEvents().isEmpty(),
                  "A projection must not expose caller uncommitted publication");
              try (var executor = Executors.newSingleThreadExecutor()) {
                assertTrue(
                    executor
                        .submit(() -> published.publishedEvents().isEmpty())
                        .get(10, TimeUnit.SECONDS));
              } catch (Exception e) {
                throw new AssertionError(e);
              }
            });
    var projections = published.publishedEvents();
    assertEquals(1, projections.size());
    var event = projections.getFirst();
    assertEquals(id, event.id());
    assertEquals("org", event.organizationRef());
    assertEquals(EventState.PUBLISHED, event.state());
    assertEquals(2, event.version());
    assertEquals(1, event.activities().size());
    assertEquals(event.publishedAt(), event.activities().getFirst().effectiveRegistrationOpen());
    assertEquals(ZoneOffset.ofHours(2), event.activities().getFirst().start().getOffset());
    assertTrue(projections.stream().noneMatch(e -> e.id().equals(draft)));
    UUID rollback = ready();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              service.publish(rollback, 1);
              status.setRollbackOnly();
            });
    assertEquals(
        List.of(id),
        published.publishedEvents().stream()
            .map(PublishedEventsReadContract.PublishedEvent::id)
            .toList());
  }

  @Test
  void US17_concurrentPublishesSerializeAndOnlyOneWins() throws Exception {
    UUID id = ready();
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Integer> attempt =
          () -> {
            start.await();
            try {
              service.publish(id, 1);
              return 200;
            } catch (AuthoringException e) {
              return e.status();
            }
          };
      var first = executor.submit(attempt);
      var second = executor.submit(attempt);
      start.countDown();
      assertEquals(
          Set.of(200, 409),
          Set.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)));
    }
    assertEquals(2, service.getEvent(id).version());
    assertEquals("Published", service.getEvent(id).state());
  }
}
