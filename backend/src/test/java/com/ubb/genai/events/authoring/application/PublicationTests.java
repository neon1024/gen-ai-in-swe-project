package com.ubb.genai.events.authoring.application;

import static org.junit.jupiter.api.Assertions.*;

import com.ubb.genai.events.authoring.domain.*;
import com.ubb.genai.events.authoring.persistence.AuthoringEventRepository;
import com.ubb.genai.events.authoring.ports.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PublicationTests {
  String subject = "owner";
  OrganizationAccessPort.Role org = OrganizationAccessPort.Role.ORGANIZER;
  EventTeamAccessPort.Role team = EventTeamAccessPort.Role.NONE;
  final Map<UUID, AuthoringEvent> rows = new HashMap<>();
  final Instant now = Instant.parse("2026-11-10T10:00:00.123456789Z");
  EventAuthoringService service;

  @BeforeEach
  void setup() {
    service =
        new EventAuthoringService(
            () -> new AuthoringIdentityPort.Actor(subject, true),
            (s, o) -> org,
            (s, e) -> team,
            new AuthoringEventRepository() {
              public AuthoringEvent save(AuthoringEvent e) {
                rows.put(e.id(), e);
                return e;
              }

              public Optional<AuthoringEvent> findById(UUID id) {
                return Optional.ofNullable(rows.get(id));
              }

              public AuthoringEvent replace(AuthoringEvent e) {
                var next =
                    new AuthoringEvent(
                        e.id(),
                        e.organizationRef(),
                        e.ownerSubject(),
                        e.title(),
                        e.description(),
                        e.category(),
                        e.state(),
                        e.createdAt(),
                        e.publishedAt(),
                        e.version() + 1,
                        e.activities());
                rows.put(next.id(), next);
                return next;
              }
            },
            Clock.fixed(now, ZoneOffset.UTC));
  }

  AuthoringActivity activity() {
    return new AuthoringActivity(
        UUID.randomUUID(),
        "Session",
        "Details",
        new ActivitySchedule(
            OffsetDateTime.parse("2026-12-01T10:00:00+02:00"),
            OffsetDateTime.parse("2026-12-01T12:00:00+02:00")),
        "Room",
        null,
        10,
        new RegistrationWindow(null, null),
        ActivityVisibility.PUBLIC,
        false,
        now.minusSeconds(60));
  }

  AuthoringEvent event(boolean withActivity) {
    var e =
        AuthoringEvent.draft(
            "org", "owner", "Event", "Details", EventCategory.WORKSHOP, now.minusSeconds(120));
    if (withActivity) e = e.withActivities(List.of(activity()));
    rows.put(e.id(), e);
    return e;
  }

  @Test
  void US17_emptyDraftReturnsReadiness422WithoutMutation() {
    var e = event(false);
    var failure = assertThrows(AuthoringException.class, () -> service.publish(e.id(), 0));
    assertEquals(422, failure.status());
    assertTrue(failure.violations().stream().anyMatch(v -> v.path().equals("activities")));
    assertEquals(e, rows.get(e.id()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Owner", "Editor", "Admin"})
  void US17_authorizedRolesPublishAndKeepOriginalTimestamp(String role) {
    var e = event(true);
    if (role.equals("Editor")) {
      subject = "editor";
      team = EventTeamAccessPort.Role.EDITOR;
      org = OrganizationAccessPort.Role.MEMBER;
    }
    if (role.equals("Admin")) {
      subject = "admin";
      org = OrganizationAccessPort.Role.ORG_ADMIN;
    }
    var published = assertDoesNotThrow(() -> service.publish(e.id(), 0));
    assertEquals("Published", published.state());
    assertEquals(now.truncatedTo(java.time.temporal.ChronoUnit.MICROS), published.publishedAt());
    assertEquals(1, published.version());
    assertEquals(
        409, assertThrows(AuthoringException.class, () -> service.publish(e.id(), 1)).status());
    assertEquals(published.publishedAt(), rows.get(e.id()).publishedAt());
  }

  @Test
  void US17_staffDeniedBeforeVersionAndReadiness() {
    var e = event(false);
    subject = "staff";
    org = OrganizationAccessPort.Role.MEMBER;
    team = EventTeamAccessPort.Role.STAFF;
    assertEquals(
        403, assertThrows(AuthoringException.class, () -> service.publish(e.id(), 9)).status());
  }

  @Test
  void US17_staleVersionAndTerminalStatesRefused() {
    var e = event(true);
    assertEquals(
        409, assertThrows(AuthoringException.class, () -> service.publish(e.id(), 1)).status());
    for (var state : List.of(EventState.COMPLETED, EventState.CANCELLED)) {
      rows.put(
          e.id(),
          new AuthoringEvent(
              e.id(),
              e.organizationRef(),
              e.ownerSubject(),
              e.title(),
              e.description(),
              e.category(),
              state,
              e.createdAt(),
              now,
              0,
              e.activities()));
      assertEquals(
          409, assertThrows(AuthoringException.class, () -> service.publish(e.id(), 0)).status());
    }
  }

  @Test
  void US17_revalidatesEveryActivityAndAccumulatesAllProblems() {
    var e = event(false);
    var good = activity();
    var invalid1 =
        new AuthoringActivity(
            UUID.randomUUID(),
            " ",
            "",
            good.schedule(),
            null,
            null,
            0,
            new RegistrationWindow(good.schedule().end().plusHours(1), good.schedule().end()),
            ActivityVisibility.PUBLIC,
            false,
            good.createdAt(),
            List.of(
                new AgendaItem(UUID.randomUUID(), good.schedule().end().plusHours(1), "", null)));
    var invalid2 =
        new AuthoringActivity(
            UUID.randomUUID(), "", "", null, "Room", null, -1, null, null, false, null);
    e = e.withActivities(List.of(good, invalid1, invalid2));
    rows.put(e.id(), e);
    var id = e.id();
    var violations = PublicationReadiness.violations(e);
    assertTrue(
        violations.stream().anyMatch(v -> v.path().equals("activities[1].registrationOpen")));
    assertTrue(
        violations.stream().anyMatch(v -> v.path().equals("activities[1].registrationClose")));
    assertTrue(violations.size() >= 12, violations.toString());
    assertTrue(violations.stream().anyMatch(v -> v.path().startsWith("activities[1]")));
    assertTrue(violations.stream().anyMatch(v -> v.path().startsWith("activities[2]")));
    var failure = assertThrows(AuthoringException.class, () -> service.publish(id, 0));
    assertEquals(422, failure.status());
    assertEquals(violations, failure.violations());
  }
}
