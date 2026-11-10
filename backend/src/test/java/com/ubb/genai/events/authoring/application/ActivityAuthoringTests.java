package com.ubb.genai.events.authoring.application;

import static org.junit.jupiter.api.Assertions.*;

import com.ubb.genai.events.authoring.domain.*;
import com.ubb.genai.events.authoring.ports.*;
import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import({PostgresTestConfiguration.class, ActivityAuthoringTests.Ports.class})
class ActivityAuthoringTests {
  @Autowired EventAuthoringService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired Guard guard;

  static class Guard implements RegistrationAuthoringPort {
    boolean history, failAfter;
    GuardRequest requested;
    int promotions;
    public <T> T withLockedActivity(GuardRequest request, Function<RegistrationContext, T> mutation) {
      requested = request;
      T result = mutation.apply(new RegistrationContext() {
        public long confirmedCount() { return 0; }
        public boolean hasAnySignup() { return history; }
        public void promoteEligibleWaitlist(int capacity, Instant start, Instant now) { promotions++; }
      });
      if (failAfter) throw AuthoringException.unavailable();
      return result;
    }
  }
  @TestConfiguration static class Ports {
    @Bean AuthoringIdentityPort identity() { return () -> new AuthoringIdentityPort.Actor("owner", true); }
    @Bean OrganizationAccessPort organizations() { return (s, o) -> OrganizationAccessPort.Role.ORGANIZER; }
    @Bean EventTeamAccessPort team() { return (s, e) -> EventTeamAccessPort.Role.NONE; }
    @Bean Guard guard() { return new Guard(); }
    @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC); }
  }
  @BeforeEach void reset() {
    guard.history = false;
    guard.failAfter = false;
    guard.requested = null;
    guard.promotions = 0;
    jdbc.update("delete from event_authoring.agenda_item");
    jdbc.update("delete from event_authoring.activity");
    jdbc.update("delete from event_authoring.event");
  }
  EventAuthoringService.ActivityCommand command(long version) {
    return new EventAuthoringService.ActivityCommand("Title", "Description",
        OffsetDateTime.parse("2026-12-01T10:00:00+02:00"), OffsetDateTime.parse("2026-12-01T12:00:00+02:00"),
        null, "online meeting", 3, null, null, null, version);
  }
  UUID event() {
    return service.createDraft(new EventAuthoringService.CreateEventCommand("Event", "Details", EventCategory.WORKSHOP, "org")).id();
  }
  @Test void US15_guardDeniesAnyHistoricalSignupEvenWithNoConfirmedPeople() {
    UUID event = event();
    var activity = service.addActivity(event, command(0)).activities().getFirst();
    guard.history = true;
    assertEquals(409, assertThrows(AuthoringException.class, () -> service.removeActivity(event, activity.id(), 1)).status());
    assertEquals(1, service.getEvent(event).version());
    assertEquals(1, service.getEvent(event).activities().size());
    assertEquals(new RegistrationAuthoringPort.GuardRequest(event, activity.id(), RegistrationAuthoringPort.MutationKind.REMOVE, 1L, null), guard.requested);
    assertEquals(0, guard.promotions);
  }
  @Test void US15_removalCommitsNextRootVersionOnlyWithinGuard() {
    UUID event = event();
    var activity = service.addActivity(event, command(0)).activities().getFirst();
    var result = service.removeActivity(event, activity.id(), 1);
    assertEquals(2, result.version());
    assertTrue(result.activities().isEmpty());
    assertEquals(result, service.getEvent(event));
    assertEquals(0, guard.promotions);
  }
  @Test void US15_guardFailureAfterMutationRollsBackSqlAndRootVersion() {
    UUID event = event();
    var before = service.addActivity(event, command(0));
    guard.failAfter = true;
    assertEquals(503, assertThrows(AuthoringException.class, () -> service.removeActivity(event, before.activities().getFirst().id(), 1)).status());
    assertEquals(before, service.getEvent(event));
    assertEquals(1, jdbc.queryForObject("select count(*) from event_authoring.activity", Integer.class));
    assertEquals(1L, jdbc.queryForObject("select version from event_authoring.event", Long.class));
  }
}
