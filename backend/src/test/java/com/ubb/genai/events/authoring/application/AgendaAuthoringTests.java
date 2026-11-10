package com.ubb.genai.events.authoring.application;

import static org.junit.jupiter.api.Assertions.*;

import com.ubb.genai.events.authoring.api.AuthoringViews.EventView;
import com.ubb.genai.events.authoring.domain.EventCategory;
import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import({PostgresTestConfiguration.class, ActivityAuthoringTests.Ports.class})
class AgendaAuthoringTests {
  @Autowired EventAuthoringService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired ActivityAuthoringTests.Guard guard;

  @BeforeEach void reset() {
    guard.history = false;
    guard.failAfter = false;
    guard.promotions = 0;
    jdbc.update("delete from event_authoring.agenda_item");
    jdbc.update("delete from event_authoring.activity");
    jdbc.update("delete from event_authoring.event");
  }
  EventAuthoringService.ActivityCommand activity(long version, OffsetDateTime start,
      OffsetDateTime open, OffsetDateTime close) {
    return new EventAuthoringService.ActivityCommand("Title", "Description", start,
        start.plusHours(2), "Room", null, 5, open, close, "Public", version);
  }
  EventView eventWithAgenda() {
    var event = service.createDraft(new EventAuthoringService.CreateEventCommand("Event", "Details", EventCategory.OTHER, "org"));
    var start = OffsetDateTime.parse("2026-12-01T10:00:00+02:00");
    var added = service.addActivity(event.id(), activity(0, start, null, null));
    return service.addAgenda(event.id(), added.activities().getFirst().id(),
        new EventAuthoringService.AgendaCommand(start.plusHours(1), "Talk", "Speaker", 1L));
  }
  @Test void US16_competingChildOnlyUpdatesHaveExactlyOneCommitAndOneStaleConflict() throws Exception {
    var before = eventWithAgenda();
    var activity = before.activities().getFirst();
    var item = activity.agenda().getFirst();
    var barrier = new CyclicBarrier(2);
    try (var pool = Executors.newFixedThreadPool(2)) {
      List<Future<Integer>> results = new ArrayList<>();
      for (String title : List.of("One", "Two")) {
        results.add(pool.submit(() -> {
          barrier.await(10, TimeUnit.SECONDS);
          try {
            var result = service.updateAgenda(before.id(), activity.id(), item.id(),
                new EventAuthoringService.AgendaCommand(item.time(), title, null, 2L));
            assertEquals(3, result.version());
            return 200;
          } catch (AuthoringException exception) { return exception.status(); }
        }));
      }
      var statuses = List.of(results.get(0).get(15, TimeUnit.SECONDS), results.get(1).get(15, TimeUnit.SECONDS));
      assertEquals(List.of(200, 409), statuses.stream().sorted().toList());
    }
    var after = service.getEvent(before.id());
    assertEquals(3, after.version());
    assertEquals(1, after.activities().getFirst().agenda().size());
    assertTrue(List.of("One", "Two").contains(after.activities().getFirst().agenda().getFirst().title()));
  }
  @Test void US16_draftActivityRemovalDeletesOwnedAgendaAndBumpsRootOnce() {
    var before = eventWithAgenda();
    var after = service.removeActivity(before.id(), before.activities().getFirst().id(), 2);
    assertEquals(3, after.version());
    assertTrue(after.activities().isEmpty());
    assertEquals(0, jdbc.queryForObject("select count(*) from event_authoring.agenda_item", Integer.class));
  }
  @Test void US16_guardFailureRollsBackPublishedRemovalOfActivityAgendaAndVersion() {
    var draft = eventWithAgenda();
    var withSecond = service.addActivity(draft.id(),
        activity(2, OffsetDateTime.parse("2026-12-02T10:00:00+02:00"), null, null));
    var before = service.publish(withSecond.id(), 3);
    var withAgenda = before.activities().stream().filter(a -> !a.agenda().isEmpty()).findFirst().orElseThrow();
    guard.failAfter = true;
    assertEquals(503, assertThrows(AuthoringException.class,
        () -> service.removeActivity(before.id(), withAgenda.id(), 4)).status());
    assertEquals(before, service.getEvent(before.id()));
    assertEquals(1, jdbc.queryForObject("select count(*) from event_authoring.agenda_item", Integer.class));
    assertEquals(4L, jdbc.queryForObject("select version from event_authoring.event", Long.class));
  }
  @Test void BR08_defaultCloseFollowsScheduleButExplicitWindowAndAgendaStayUnchanged() {
    var before = eventWithAgenda();
    var original = before.activities().getFirst();
    var movedStart = original.start().minusHours(1);
    var changed = service.updateActivity(before.id(), original.id(), activity(2, movedStart, null, null));
    var moved = changed.activities().getFirst();
    assertNull(moved.registrationOpen());
    assertNull(moved.registrationClose());
    assertNull(moved.effectiveRegistrationOpen());
    assertEquals(movedStart.toInstant(), moved.effectiveRegistrationClose());
    assertEquals(original.agenda(), moved.agenda());
    var open = original.start().minusDays(2);
    var close = original.start().minusHours(2);
    var explicit = service.updateActivity(before.id(), original.id(), activity(3, movedStart, open, close));
    assertEquals(open, explicit.activities().getFirst().registrationOpen());
    assertEquals(close, explicit.activities().getFirst().registrationClose());
    assertEquals(original.agenda(), explicit.activities().getFirst().agenda());
  }
}
