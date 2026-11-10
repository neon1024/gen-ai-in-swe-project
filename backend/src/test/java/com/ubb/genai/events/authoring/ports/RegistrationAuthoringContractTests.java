package com.ubb.genai.events.authoring.ports;

import static org.junit.jupiter.api.Assertions.*;

import com.ubb.genai.events.authoring.api.AuthoringViews.EventView;
import com.ubb.genai.events.authoring.application.*;
import com.ubb.genai.events.authoring.domain.*;
import com.ubb.genai.events.authoring.support.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import({PostgresTestConfiguration.class, PublishedMutationPorts.class})
class RegistrationAuthoringContractTests {
  @Autowired EventAuthoringService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired PublishedMutationPorts.Probe probe;
  final OffsetDateTime start = OffsetDateTime.parse("2026-12-01T10:00:00+02:00");

  @BeforeEach
  void reset() {
    probe.reset();
    jdbc.update("delete from event_authoring.agenda_item");
    jdbc.update("delete from event_authoring.activity");
    jdbc.update("delete from event_authoring.event");
  }

  EventAuthoringService.ActivityCommand command(long version, int capacity, OffsetDateTime at) {
    return new EventAuthoringService.ActivityCommand(
        "Session",
        "Details",
        at,
        at.plusHours(2),
        "Room",
        null,
        capacity,
        null,
        null,
        null,
        version);
  }

  EventView ready(boolean published) {
    var e =
        service.createDraft(
            new EventAuthoringService.CreateEventCommand(
                "Event", "Details", EventCategory.WORKSHOP, "org"));
    e = service.addActivity(e.id(), command(0, 10, start));
    return published ? service.publish(e.id(), 1) : e;
  }

  @Test
  void BR12_capacityCannotFallBelowAuthoritativeConfirmedCount() {
    var before = ready(true);
    probe.confirmed = 9;
    probe.history = true;
    assertEquals(
        409,
        assertThrows(
                AuthoringException.class,
                () ->
                    service.updateActivity(
                        before.id(),
                        before.activities().getFirst().id(),
                        command(before.version(), 8, start)))
            .status());
    assertEquals(before, service.getEvent(before.id()));
    assertEquals(1, probe.calls);
    assertEquals(0, probe.promotions);
  }

  @ParameterizedTest
  @ValueSource(ints = {3, 12})
  void EA03_draftCapacityChangesNeverInvokeRegistrationGuard(int capacity) {
    var before = ready(false);
    probe.unavailable = true;
    probe.confirmed = 9;
    var after =
        service.updateActivity(
            before.id(), before.activities().getFirst().id(), command(1, capacity, start));
    assertEquals(capacity, after.activities().getFirst().capacity());
    assertEquals(2, after.version());
    assertEquals("Draft", after.state());
    assertEquals(0, probe.calls);
    assertEquals(0, probe.promotions);
  }

  @Test
  void EA03_draftActivityRemovalNeverInvokesRegistrationGuard() {
    var draft = ready(false);
    var activity = draft.activities().getFirst();
    var before =
        service.addAgenda(
            draft.id(),
            activity.id(),
            new EventAuthoringService.AgendaCommand(start, "Talk", null, 1L));
    probe.unavailable = true;
    probe.history = true;
    var after = assertDoesNotThrow(() -> service.removeActivity(before.id(), activity.id(), 2));
    assertTrue(after.activities().isEmpty());
    assertEquals(3, after.version());
    assertEquals("Draft", after.state());
    assertEquals(0, probe.calls);
    assertNull(probe.requested);
    assertEquals(
        0,
        jdbc.queryForObject("select count(*) from event_authoring.agenda_item", Integer.class));
  }

  @Test
  void BR12_everyPublishedCapacityChangeIsGuardedAndIncreasePromotesInTransaction() {
    var before = ready(true);
    probe.confirmed = 7;
    probe.history = true;
    var after =
        service.updateActivity(
            before.id(), before.activities().getFirst().id(), command(2, 12, start));
    assertEquals(12, after.activities().getFirst().capacity());
    assertEquals(1, probe.calls);
    assertEquals(
        new RegistrationAuthoringPort.GuardRequest(
            before.id(),
            before.activities().getFirst().id(),
            RegistrationAuthoringPort.MutationKind.CAPACITY_CHANGE,
            2L,
            12),
        probe.requested);
    assertEquals(1, probe.promotions);
    assertEquals(12, probe.promotedCapacity);
    assertTrue(probe.guardedTransaction);
    assertTrue(probe.promotedTransaction);
    assertEquals(start.toInstant(), probe.promotionStart);
    assertEquals(probe.now, probe.promotionNow);
    service.updateActivity(before.id(), before.activities().getFirst().id(), command(3, 7, start));
    assertEquals(2, probe.calls);
    assertEquals(3L, probe.requested.expectedVersion());
    assertEquals(7, probe.requested.requestedCapacity());
    assertEquals(1, probe.promotions);
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-12-01T08:00:00Z", "2026-12-01T09:00:00Z"})
  void BR12_neverPromotesAtOrAfterUpdatedStart(String now) {
    var before = ready(true);
    probe.now = Instant.parse(now);
    service.updateActivity(before.id(), before.activities().getFirst().id(), command(2, 12, start));
    assertEquals(1, probe.calls);
    assertEquals(0, probe.promotions);
  }

  @Test
  void BR12_clockIsEvaluatedAfterGuardLocksAndUsesUpdatedStart() {
    var before = ready(true);
    probe.now = start.toInstant().minusSeconds(60);
    probe.lockedNow = start.toInstant();
    service.updateActivity(before.id(), before.activities().getFirst().id(), command(2, 12, start));
    assertEquals(0, probe.promotions);
    probe.lockedNow = start.toInstant().plusSeconds(60);
    var updated = start.plusHours(1);
    service.updateActivity(
        before.id(), before.activities().getFirst().id(), command(3, 14, updated));
    assertEquals(1, probe.promotions);
    assertEquals(updated.toInstant(), probe.promotionStart);
    assertEquals(probe.lockedNow, probe.promotionNow);
  }

  @Test
  void BR12_timeAndDetailsWithUnchangedCapacityPreserveSignupsWithoutGuard() {
    var before = ready(true);
    probe.confirmed = 10;
    probe.history = true;
    probe.unavailable = true;
    var after =
        service.updateActivity(
            before.id(),
            before.activities().getFirst().id(),
            command(2, 10, start.plusMinutes(30)));
    assertEquals(start.plusMinutes(30), after.activities().getFirst().start());
    assertEquals(0, probe.calls);
    assertEquals(0, probe.promotions);
  }

  @ParameterizedTest
  @ValueSource(strings = {"missing", "promotion", "after"})
  void BR12_guardOrPromotionFailureRollsBackAuthoringRowsAndVersion(String failure) {
    var initial = ready(true);
    var activity = initial.activities().getFirst();
    var before =
        service.addAgenda(
            initial.id(),
            activity.id(),
            new EventAuthoringService.AgendaCommand(start, "Talk", null, 2L));
    probe.unavailable = failure.equals("missing");
    probe.failPromotion = failure.equals("promotion");
    probe.failAfter = failure.equals("after");
    assertEquals(
        503,
        assertThrows(
                AuthoringException.class,
                () -> service.updateActivity(before.id(), activity.id(), command(3, 12, start)))
            .status());
    assertEquals(before, service.getEvent(before.id()));
    assertEquals(
        10,
        jdbc.queryForObject(
            "select capacity from event_authoring.activity where id=?",
            Integer.class,
            activity.id()));
    assertEquals(
        3L,
        jdbc.queryForObject(
            "select version from event_authoring.event where id=?", Long.class, before.id()));
  }

  @Test
  void BR14_anySignupHistoryBlocksRemovalWithZeroConfirmed() {
    var before = ready(true);
    var added = service.addActivity(before.id(), command(2, 10, start));
    var id = before.activities().getFirst().id();
    probe.history = true;
    probe.confirmed = 0;
    assertEquals(
        409,
        assertThrows(AuthoringException.class, () -> service.removeActivity(before.id(), id, 3))
            .status());
    assertEquals(added, service.getEvent(before.id()));
    assertEquals(
        new RegistrationAuthoringPort.GuardRequest(
            before.id(), id, RegistrationAuthoringPort.MutationKind.REMOVE, 3L, null),
        probe.requested);
    assertEquals(0, probe.promotions);
  }

  @Test
  void BR12_staleAndConcurrentCapacityWritesCannotBypassRootVersion() throws Exception {
    var before = ready(true);
    var id = before.activities().getFirst().id();
    var latch = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> change =
          () -> {
            latch.await();
            try {
              service.updateActivity(before.id(), id, command(2, 12, start));
              return 200;
            } catch (AuthoringException e) {
              return e.status();
            }
          };
      var first = pool.submit(change);
      var second = pool.submit(change);
      latch.countDown();
      var outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
      assertTrue(outcomes.containsAll(List.of(200, 409)), outcomes.toString());
    }
    assertEquals(1, probe.calls);
    assertEquals(1, probe.promotions);
    assertEquals(3, service.getEvent(before.id()).version());
    assertEquals(
        409,
        assertThrows(
                AuthoringException.class,
                () -> service.updateActivity(before.id(), id, command(2, 14, start)))
            .status());
    assertEquals(1, probe.calls);
  }
}
