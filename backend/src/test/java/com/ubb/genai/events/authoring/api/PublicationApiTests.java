package com.ubb.genai.events.authoring.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ubb.genai.events.authoring.application.EventAuthoringService;
import com.ubb.genai.events.authoring.domain.EventCategory;
import com.ubb.genai.events.authoring.ports.OrganizationAccessPort;
import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestConfiguration.class, DraftAuthoringApiTests.TrustedPorts.class})
class PublicationApiTests {
  @Autowired MockMvc mvc;
  @Autowired EventAuthoringService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired DraftAuthoringApiTests.Fixture fixture;
  static final String BASE = "/api/event-authoring/events/";

  @BeforeEach
  void reset() {
    fixture.subject = "creator";
    fixture.eligible = true;
    fixture.unavailable = false;
    fixture.organizations.clear();
    fixture.teams.clear();
    fixture.organization("creator", "org", OrganizationAccessPort.Role.ORGANIZER);
    jdbc.update("delete from event_authoring.agenda_item");
    jdbc.update("delete from event_authoring.activity");
    jdbc.update("delete from event_authoring.event");
  }

  UUID event(boolean activity) {
    var e =
        service.createDraft(
            new EventAuthoringService.CreateEventCommand(
                "Event", "Details", EventCategory.WORKSHOP, "org"));
    if (activity)
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
  void US17_emptyReadinessUses422Envelope() throws Exception {
    var id = event(false);
    mvc.perform(
            post(BASE + id + "/publish")
                .with(user("session"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"expectedVersion\":0}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.violations[0].path").value("activities"));
    assertEquals("Draft", service.getEvent(id).state());
  }

  @Test
  void US17_publicationPersistsTimestampWindowsAndVersion() throws Exception {
    var id = event(true);
    var response =
        mvc.perform(
                post(BASE + id + "/publish")
                    .with(user("session"))
                    .with(csrf())
                    .contentType("application/json")
                    .content("{\"expectedVersion\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("Published"))
            .andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.publishedAt").value("2026-11-10T10:00:00.123456Z"))
            .andExpect(
                jsonPath("$.activities[0].effectiveRegistrationOpen")
                    .value("2026-11-10T10:00:00.123456Z"))
            .andExpect(jsonPath("$.ownerSubject").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    mvc.perform(get(BASE + id).with(user("session")))
        .andExpect(status().isOk())
        .andExpect(content().json(response));
  }

  @Test
  void US18_missingProductionRegistrationAdapterRefusesCapacityChange() {
    var id = event(true);
    var before = service.publish(id, 1);
    var activity = before.activities().getFirst();
    var failure =
        assertThrows(
            com.ubb.genai.events.authoring.application.AuthoringException.class,
            () ->
                service.updateActivity(
                    id,
                    activity.id(),
                    new EventAuthoringService.ActivityCommand(
                        activity.title(),
                        activity.description(),
                        activity.start(),
                        activity.end(),
                        activity.location(),
                        activity.onlineLink(),
                        12,
                        activity.registrationOpen(),
                        activity.registrationClose(),
                        activity.visibility(),
                        2L)));
    assertEquals(503, failure.status());
    assertEquals(before, service.getEvent(id));
  }

  @Test
  void US17_and_US18_sessionCsrfAndStaffAuthorizationRemainDistinct() throws Exception {
    var id = event(true);
    var version = "{\"expectedVersion\":1}";
    mvc.perform(
            post(BASE + id + "/publish")
                .with(csrf())
                .contentType("application/json")
                .content(version))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post(BASE + id + "/publish")
                .with(user("session"))
                .contentType("application/json")
                .content(version))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_REJECTED"));
    fixture.subject = "staff";
    fixture.organization("staff", "org", OrganizationAccessPort.Role.MEMBER);
    fixture.team(
        "staff",
        id.toString(),
        com.ubb.genai.events.authoring.ports.EventTeamAccessPort.Role.STAFF);
    mvc.perform(
            post(BASE + id + "/publish")
                .with(user("session"))
                .with(csrf())
                .contentType("application/json")
                .content(version))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    mvc.perform(
            put(BASE + id)
                .with(user("session"))
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"expectedVersion\":1,\"title\":\"Changed\",\"description\":\"Details\",\"category\":\"Other\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void openApiDocumentsPublicationAndMetadataContracts() throws Exception {
    mvc.perform(get("/v3/api-docs").with(user("session")))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.paths['/api/event-authoring/events/{eventId}/publish'].post.responses['200'].content['application/json'].schema['$ref']")
                .value("#/components/schemas/EventView"))
        .andExpect(
            jsonPath(
                    "$.paths['/api/event-authoring/events/{eventId}/publish'].post.responses['422'].content['application/json'].schema['$ref']")
                .value("#/components/schemas/ErrorView"))
        .andExpect(
            jsonPath(
                    "$.paths['/api/event-authoring/events/{eventId}'].put.responses['200'].content['application/json'].schema['$ref']")
                .value("#/components/schemas/EventView"))
        .andExpect(
            jsonPath("$.components.schemas.VersionCommand.required")
                .value(org.hamcrest.Matchers.hasItem("expectedVersion")))
        .andExpect(
            jsonPath("$.components.schemas.VersionCommand.properties.expectedVersion.minimum")
                .value(0))
        .andExpect(
            jsonPath("$.components.schemas.EventDetails.required")
                .value(
                    org.hamcrest.Matchers.hasItems(
                        "expectedVersion", "title", "description", "category")))
        .andExpect(
            jsonPath("$.components.schemas.EventDetails.properties.category.enum")
                .value(
                    org.hamcrest.Matchers.containsInAnyOrder(
                        "Workshop", "Conference", "Training", "Social", "Competition", "Other")));
  }

  @Test
  void US18_eventDetailsEndpointPersistsOnlyAllowedMetadata() throws Exception {
    var id = event(true);
    service.publish(id, 1);
    mvc.perform(
            put(BASE + id)
                .with(user("session"))
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"expectedVersion\":2,\"title\":\"Changed\",\"description\":\"New"
                        + " details\",\"category\":\"Social\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Changed"))
        .andExpect(jsonPath("$.category").value("Social"))
        .andExpect(jsonPath("$.version").value(3))
        .andExpect(jsonPath("$.state").value("Published"))
        .andExpect(jsonPath("$.publishedAt").value("2026-11-10T10:00:00.123456Z"));
    assertEquals("Changed", service.getEvent(id).title());
  }

  @Test
  void US18_strictDetailsMetadataVersionAndImmutableFields() throws Exception {
    var id = event(true);
    var valid =
        "{\"expectedVersion\":1,\"title\":\"Title\",\"description\":\"Details\",\"category\":\"Workshop\"}";
    for (var body :
        new String[] {
          valid.replace("1,", "null,"),
          valid.replace("1,", "-1,"),
          valid.replace("1,", "\"1\","),
          valid.replace("1,", "1.0,"),
          valid.replace("1,", "true,"),
          valid.replace("\"title\":\"Title\"", "\"title\":23"),
          valid.replace("\"description\":\"Details\"", "\"description\":true"),
          valid.replace("Workshop", "workshop"),
          valid.replace("Title", " "),
          valid.replace("Details", "before\\u0000after"),
          valid.replace("\"expectedVersion\":1,", "")
        })
      mvc.perform(
              put(BASE + id)
                  .with(user("session"))
                  .with(csrf())
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isBadRequest());
    for (var field :
        new String[] {
          "owner",
          "ownerSubject",
          "actor",
          "organizationRef",
          "state",
          "createdAt",
          "publishedAt",
          "cancelled"
        })
      mvc.perform(
              put(BASE + id)
                  .with(user("session"))
                  .with(csrf())
                  .contentType("application/json")
                  .content(
                      valid.substring(0, valid.length() - 1) + ",\"" + field + "\":\"injected\"}"))
          .andExpect(status().isBadRequest());
    assertEquals(1, service.getEvent(id).version());
  }

  @Test
  void US17_publishVersionIsStrictMandatoryAndUnknownFieldsRejected() throws Exception {
    var id = event(true);
    for (var body :
        new String[] {
          "{}",
          "{\"expectedVersion\":null}",
          "{\"expectedVersion\":-1}",
          "{\"expectedVersion\":\"1\"}",
          "{\"expectedVersion\":1.0}",
          "{\"expectedVersion\":true}",
          "{\"expectedVersion\":1,\"state\":\"Published\"}"
        })
      mvc.perform(
              post(BASE + id + "/publish")
                  .with(user("session"))
                  .with(csrf())
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isBadRequest());
  }
}
