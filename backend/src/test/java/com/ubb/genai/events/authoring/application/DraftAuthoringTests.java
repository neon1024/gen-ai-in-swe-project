package com.ubb.genai.events.authoring.application;

import static org.junit.jupiter.api.Assertions.*;

import com.ubb.genai.events.authoring.domain.*;
import com.ubb.genai.events.authoring.persistence.AuthoringEventRepository;
import com.ubb.genai.events.authoring.ports.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DraftAuthoringTests {
  String subject = "creator";
  boolean eligible = true;
  boolean unavailableIdentity = false, unavailableOrg = false, unavailableTeam = false;
  OrganizationAccessPort.Role org = OrganizationAccessPort.Role.ORGANIZER;
  EventTeamAccessPort.Role team = EventTeamAccessPort.Role.NONE;
  Map<UUID, AuthoringEvent> rows = new HashMap<>();
  Clock clock = Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC);
  EventAuthoringService service;

  @BeforeEach
  void setup() {
    service =
        new EventAuthoringService(
            () -> {
              if (unavailableIdentity) throw AuthoringException.unavailable();
              return subject == null ? null : new AuthoringIdentityPort.Actor(subject, eligible);
            },
            (s, o) -> {
              if (unavailableOrg) throw AuthoringException.unavailable();
              return org;
            },
            (s, e) -> {
              if (unavailableTeam) throw AuthoringException.unavailable();
              return team;
            },
            new AuthoringEventRepository() {
              public AuthoringEvent save(AuthoringEvent e) {
                rows.put(e.id(), e);
                return e;
              }

              public Optional<AuthoringEvent> findById(UUID id) {
                return Optional.ofNullable(rows.get(id));
              }
            },
            clock);
  }

  EventAuthoringService.CreateEventCommand command() {
    return new EventAuthoringService.CreateEventCommand(
        "Title", "Description", EventCategory.WORKSHOP, "org");
  }

  UUID create() {
    var result = service.createDraft(command());
    assertNotNull(result, "Draft creation must return a persisted view");
    return result.id();
  }

  void denied(int status, Runnable action) {
    var ex = assertThrows(AuthoringException.class, action::run);
    assertEquals(status, ex.status());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganizationAccessPort.Role.class,
      names = {"ORGANIZER", "ORG_ADMIN"})
  void US14_eligibleOrganizerOrAdminCreates(OrganizationAccessPort.Role role) {
    org = role;
    var view = service.createDraft(command());
    assertNotNull(view);
    assertEquals("creator", rows.get(view.id()).ownerSubject());
    assertEquals("Draft", view.state());
    assertEquals("Workshop", view.category());
    assertEquals(clock.instant(), view.createdAt());
    assertTrue(view.activities().isEmpty());
    assertTrue(view.capabilities().canEdit());
    assertTrue(view.capabilities().canPublish());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganizationAccessPort.Role.class,
      names = {"NONE", "MEMBER"})
  void US14_staffOrNonOrganizerCannotCreate(OrganizationAccessPort.Role role) {
    org = role;
    team = EventTeamAccessPort.Role.STAFF;
    denied(403, () -> service.createDraft(command()));
    assertTrue(rows.isEmpty());
  }

  @Test
  void missingOrIneligibleIdentityCannotCreate() {
    subject = null;
    denied(401, () -> service.createDraft(command()));
    subject = "actor";
    eligible = false;
    denied(401, () -> service.createDraft(command()));
    assertTrue(rows.isEmpty());
  }

  @Test
  void validatesAllRequiredFields() {
    for (String value : new String[] {null, "", " ", "\t"}) {
      denied(
          400,
          () ->
              service.createDraft(
                  new EventAuthoringService.CreateEventCommand(
                      value, "D", EventCategory.OTHER, "org")));
      denied(
          400,
          () ->
              service.createDraft(
                  new EventAuthoringService.CreateEventCommand(
                      "T", value, EventCategory.OTHER, "org")));
      denied(
          400,
          () ->
              service.createDraft(
                  new EventAuthoringService.CreateEventCommand(
                      "T", "D", EventCategory.OTHER, value)));
    }
    denied(
        400,
        () ->
            service.createDraft(
                new EventAuthoringService.CreateEventCommand("T", "D", null, "org")));
    assertTrue(rows.isEmpty());
  }

  @Test
  void currentOwnerReadsWithoutTeamAdapter() {
    var id = create();
    unavailableTeam = true;
    assertTrue(service.getEvent(id).capabilities().canEdit());
  }

  @Test
  void sameOrgAdminReadsWithoutTeamAdapter() {
    var id = create();
    subject = "admin";
    org = OrganizationAccessPort.Role.ORG_ADMIN;
    unavailableTeam = true;
    assertTrue(service.getEvent(id).capabilities().canEdit());
  }

  @ParameterizedTest
  @EnumSource(
      value = EventTeamAccessPort.Role.class,
      names = {"EDITOR", "STAFF"})
  void actualTeamReads(EventTeamAccessPort.Role role) {
    var id = create();
    subject = "team";
    org = OrganizationAccessPort.Role.MEMBER;
    team = role;
    var view = service.getEvent(id);
    assertNotNull(view);
    assertEquals(role == EventTeamAccessPort.Role.EDITOR, view.capabilities().canEdit());
    assertEquals(role == EventTeamAccessPort.Role.EDITOR, view.capabilities().canPublish());
  }

  @Test
  void ordinaryOrganizerAndCrossOrgConcealed() {
    var id = create();
    subject = "other";
    denied(404, () -> service.getEvent(id));
    org = OrganizationAccessPort.Role.NONE;
    team = EventTeamAccessPort.Role.EDITOR;
    denied(404, () -> service.getEvent(id));
  }

  @Test
  void formerOwnerHasNoPermanentRights() {
    var id = create();
    var e = rows.get(id);
    rows.put(
        id,
        new AuthoringEvent(
            e.id(),
            e.organizationRef(),
            "new-owner",
            e.title(),
            e.description(),
            e.category(),
            e.state(),
            e.createdAt(),
            null,
            e.version()));
    denied(404, () -> service.getEvent(id));
  }

  @Test
  void ownerStillNeedsCurrentMembershipAndEligibility() {
    var id = create();
    org = OrganizationAccessPort.Role.NONE;
    denied(404, () -> service.getEvent(id));
    org = OrganizationAccessPort.Role.ORGANIZER;
    eligible = false;
    denied(401, () -> service.getEvent(id));
    subject = null;
    denied(401, () -> service.getEvent(id));
  }

  @Test
  void missingAndConcealedHaveSameError() {
    var id = create();
    subject = "other";
    var absent = assertThrows(AuthoringException.class, () -> service.getEvent(UUID.randomUUID()));
    var concealed = assertThrows(AuthoringException.class, () -> service.getEvent(id));
    assertEquals(absent.code(), concealed.code());
    assertEquals(absent.getMessage(), concealed.getMessage());
  }

  @Test
  void mandatoryAdaptersFailClosed() {
    unavailableIdentity = true;
    denied(503, () -> service.createDraft(command()));
    unavailableIdentity = false;
    unavailableOrg = true;
    denied(503, () -> service.createDraft(command()));
    unavailableOrg = false;
    var id = create();
    subject = "other";
    unavailableTeam = true;
    denied(503, () -> service.getEvent(id));
    unavailableTeam = false;
    unavailableOrg = true;
    denied(503, () -> service.getEvent(id));
  }
}
