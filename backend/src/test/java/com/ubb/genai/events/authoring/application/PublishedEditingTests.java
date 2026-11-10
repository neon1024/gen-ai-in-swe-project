package com.ubb.genai.events.authoring.application;

import static org.junit.jupiter.api.Assertions.*;

import com.ubb.genai.events.authoring.api.AuthoringViews.EventView;
import com.ubb.genai.events.authoring.domain.*;
import com.ubb.genai.events.authoring.ports.*;
import com.ubb.genai.events.authoring.support.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import({PostgresTestConfiguration.class, PublishedMutationPorts.class})
class PublishedEditingTests {
  @Autowired EventAuthoringService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired PublishedMutationPorts.Probe probe;

  @BeforeEach
  void reset() {
    probe.reset();
    jdbc.update("delete from event_authoring.agenda_item");
    jdbc.update("delete from event_authoring.activity");
    jdbc.update("delete from event_authoring.event");
  }

  EventAuthoringService.ActivityCommand command(long version) {
    return new EventAuthoringService.ActivityCommand(
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
        version);
  }

  EventView ready(boolean published) {
    var e =
        service.createDraft(
            new EventAuthoringService.CreateEventCommand(
                "Event", "Details", EventCategory.WORKSHOP, "org"));
    e = service.addActivity(e.id(), command(0));
    return published ? service.publish(e.id(), 1) : e;
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void US18_detailsEditPreservesImmutableMetadataAndActivities(boolean published) {
    var before = ready(published);
    probe.now = probe.now.plusSeconds(60);
    var after =
        assertDoesNotThrow(
            () ->
                service.editEventDetails(
                    before.id(),
                    new EventAuthoringService.EventDetailsCommand(
                        before.version(), "Changed", "New details", EventCategory.CONFERENCE)));
    assertEquals("Changed", after.title());
    assertEquals("New details", after.description());
    assertEquals("Conference", after.category());
    assertEquals(before.version() + 1, after.version());
    assertEquals(before.publishedAt(), after.publishedAt());
    assertEquals(before.createdAt(), after.createdAt());
    assertEquals(before.organizationRef(), after.organizationRef());
    assertEquals(before.activities(), after.activities());
    assertEquals(
        "owner",
        jdbc.queryForObject(
            "select owner_subject from event_authoring.event where id=?",
            String.class,
            before.id()));
    assertEquals(after, service.getEvent(before.id()));
    assertEquals(0, probe.calls);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Editor", "Admin"})
  void US18_editorAndSameOrganizationAdminCanEditMetadata(String role) {
    var before = ready(true);
    probe.subject = "other";
    probe.organization =
        role.equals("Admin")
            ? OrganizationAccessPort.Role.ORG_ADMIN
            : OrganizationAccessPort.Role.MEMBER;
    probe.team = EventTeamAccessPort.Role.EDITOR;
    assertEquals(
        "Changed",
        assertDoesNotThrow(
                () ->
                    service.editEventDetails(
                        before.id(),
                        new EventAuthoringService.EventDetailsCommand(
                            before.version(), "Changed", "Details", EventCategory.SOCIAL)))
            .title());
  }

  @Test
  void US18_staffAndStaleDetailsEditAreRejected() {
    var before = ready(true);
    probe.subject = "staff";
    probe.organization = OrganizationAccessPort.Role.MEMBER;
    probe.team = EventTeamAccessPort.Role.STAFF;
    assertEquals(
        403,
        assertThrows(
                AuthoringException.class,
                () ->
                    service.editEventDetails(
                        before.id(),
                        new EventAuthoringService.EventDetailsCommand(
                            99L, "Changed", "Details", EventCategory.SOCIAL)))
            .status());
    probe.subject = "owner";
    assertEquals(
        409,
        assertThrows(
                AuthoringException.class,
                () ->
                    service.editEventDetails(
                        before.id(),
                        new EventAuthoringService.EventDetailsCommand(
                            0L, "Changed", "Details", EventCategory.SOCIAL)))
            .status());
  }

  @Test
  void US18_metadataValidationRejectsNullBlankNulAndMissingVersion() {
    var before = ready(true);
    for (var value : new String[] {null, " ", "before\0after"}) {
      assertEquals(
          400,
          assertThrows(
                  AuthoringException.class,
                  () ->
                      service.editEventDetails(
                          before.id(),
                          new EventAuthoringService.EventDetailsCommand(
                              2L, value, "Details", EventCategory.SOCIAL)))
              .status());
      assertEquals(
          400,
          assertThrows(
                  AuthoringException.class,
                  () ->
                      service.editEventDetails(
                          before.id(),
                          new EventAuthoringService.EventDetailsCommand(
                              2L, "Title", value, EventCategory.SOCIAL)))
              .status());
    }
    assertEquals(
        400,
        assertThrows(
                AuthoringException.class,
                () ->
                    service.editEventDetails(
                        before.id(),
                        new EventAuthoringService.EventDetailsCommand(
                            null, "Title", "Details", EventCategory.SOCIAL)))
            .status());
    assertEquals(
        400,
        assertThrows(
                AuthoringException.class,
                () ->
                    service.editEventDetails(
                        before.id(),
                        new EventAuthoringService.EventDetailsCommand(
                            2L, "Title", "Details", null)))
            .status());
    assertEquals(before, service.getEvent(before.id()));
  }

  @Test
  void US17_latePublicationAndPastPublishedActivityKeepClosedDerivedWindows() {
    var before = ready(false);
    probe.now = before.activities().getFirst().end().toInstant().plusSeconds(60);
    var published = service.publish(before.id(), 1);
    var activity = published.activities().getFirst();
    assertTrue(activity.effectiveRegistrationOpen().isAfter(activity.effectiveRegistrationClose()));
    assertEquals(probe.now, published.publishedAt());
    probe.now = probe.now.plusSeconds(60);
    var added = service.addActivity(before.id(), command(2));
    var newActivity =
        added.activities().stream()
            .filter(a -> !a.id().equals(activity.id()))
            .findFirst()
            .orElseThrow();
    assertEquals(newActivity.createdAt(), newActivity.effectiveRegistrationOpen());
    assertTrue(
        newActivity.effectiveRegistrationOpen().isAfter(newActivity.effectiveRegistrationClose()));
  }

  @Test
  void US18_lastPublishedActivityCannotBeRemovedEvenWithoutSignups() {
    var before = ready(true);
    assertEquals(
        409,
        assertThrows(
                AuthoringException.class,
                () -> service.removeActivity(before.id(), before.activities().getFirst().id(), 2))
            .status());
    assertEquals(before, service.getEvent(before.id()));
  }

  @Test
  void US18_publishedActivitiesAndAgendaRemainEditableWithPreservedAgenda() {
    var before = ready(true);
    var id = before.activities().getFirst().id();
    var time = before.activities().getFirst().start();
    var agenda =
        service.addAgenda(
            before.id(), id, new EventAuthoringService.AgendaCommand(time, "Talk", "Speaker", 2L));
    var edited = service.updateActivity(before.id(), id, command(3));
    assertEquals(agenda.activities().getFirst().agenda(), edited.activities().getFirst().agenda());
    var item = edited.activities().getFirst().agenda().getFirst().id();
    var changed =
        service.updateAgenda(
            before.id(),
            id,
            item,
            new EventAuthoringService.AgendaCommand(
                time.plusMinutes(30), "Changed talk", null, 4L));
    assertEquals("Changed talk", changed.activities().getFirst().agenda().getFirst().title());
    var removed = service.removeAgenda(before.id(), id, item, 5);
    assertTrue(removed.activities().getFirst().agenda().isEmpty());
    probe.now = probe.now.plusSeconds(30);
    var added = service.addActivity(before.id(), command(6));
    assertEquals(2, added.activities().size());
    var newActivity =
        added.activities().stream().filter(a -> !a.id().equals(id)).findFirst().orElseThrow();
    assertEquals(newActivity.createdAt(), newActivity.effectiveRegistrationOpen());
    var result = service.removeActivity(before.id(), newActivity.id(), 7);
    assertEquals(1, result.activities().size());
    assertEquals(before.publishedAt(), result.publishedAt());
    assertEquals(0, probe.promotions);
  }

  @ParameterizedTest
  @ValueSource(strings = {"COMPLETED", "CANCELLED"})
  void US18_terminalEventsRejectEveryAuthoringMutation(String state) {
    var before = ready(true);
    var id = before.id();
    var activity = before.activities().getFirst();
    var withAgenda =
        service.addAgenda(
            id,
            activity.id(),
            new EventAuthoringService.AgendaCommand(activity.start(), "Talk", null, 2L));
    var item = withAgenda.activities().getFirst().agenda().getFirst().id();
    jdbc.update("update event_authoring.event set state=? where id=?", state, id);
    for (Runnable mutation :
        List.<Runnable>of(
            () ->
                service.editEventDetails(
                    id,
                    new EventAuthoringService.EventDetailsCommand(
                        3L, "Changed", "Details", EventCategory.OTHER)),
            () -> service.addActivity(id, command(3)),
            () -> service.updateActivity(id, activity.id(), command(3)),
            () -> service.removeActivity(id, activity.id(), 3),
            () ->
                service.addAgenda(
                    id,
                    activity.id(),
                    new EventAuthoringService.AgendaCommand(activity.start(), "Talk", null, 3L)),
            () ->
                service.updateAgenda(
                    id,
                    activity.id(),
                    item,
                    new EventAuthoringService.AgendaCommand(activity.start(), "Talk", null, 3L)),
            () -> service.removeAgenda(id, activity.id(), item, 3),
            () -> service.publish(id, 3)))
      assertEquals(409, assertThrows(AuthoringException.class, mutation::run).status());
    assertEquals(3, service.getEvent(id).version());
    assertEquals(0, probe.calls);
  }
}
