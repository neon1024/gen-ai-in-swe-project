package com.ubb.genai.events.authoring.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.ubb.genai.events.authoring.ports.*;
import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestConfiguration.class, DraftAuthoringApiTests.TrustedPorts.class})
class ActivityAgendaApiTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired DraftAuthoringApiTests.Fixture fixture;
  static final String BASE = DraftAuthoringApiTests.BASE;
  static final String ACTIVITY = """
      {"expectedVersion":0,"title":"Session","description":"Hands on","start":"2026-12-01T10:00:00+02:00","end":"2026-12-01T12:00:00+03:00","location":"A","onlineLink":null,"capacity":25,"registrationOpen":null,"registrationClose":null,"visibility":"Public"}
      """;

  @BeforeEach void reset() {
    fixture.subject = "creator";
    fixture.eligible = true;
    fixture.unavailable = false;
    fixture.organizations.clear();
    fixture.teams.clear();
    fixture.organization("creator", "opaque-org", OrganizationAccessPort.Role.ORGANIZER);
    jdbc.update("delete from event_authoring.agenda_item");
    jdbc.update("delete from event_authoring.activity");
    jdbc.update("delete from event_authoring.event");
  }
  ResultActions write(String method, String path, String body) throws Exception {
    var request = switch (method) {
      case "POST" -> post(path);
      case "PUT" -> put(path);
      default -> delete(path);
    };
    return mvc.perform(request.with(user("session-user")).with(csrf()).contentType("application/json").content(body));
  }
  String event() throws Exception {
    String body = write("POST", BASE, DraftAuthoringApiTests.BODY).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return JsonPath.read(body, "$.id");
  }
  String add(String event) throws Exception {
    return write("POST", BASE + "/" + event + "/activities", ACTIVITY).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  }
  String activityId(String body) { return JsonPath.read(body, "$.activities[0].id"); }
  void noActivities() {
    assertEquals(0, jdbc.queryForObject("select count(*) from event_authoring.activity", Integer.class));
    assertEquals(0L, jdbc.queryForObject("select version from event_authoring.event", Long.class));
  }

  @Test void US15_completeWriteRoundTripsOffsetsAndVersion() throws Exception {
    String event = event();
    String body = add(event);
    String activity = activityId(body);
    assertEquals(1, (Integer) JsonPath.read(body, "$.version"));
    assertEquals("2026-12-01T10:00:00+02:00", JsonPath.read(body, "$.activities[0].start"));
    assertEquals("2026-12-01T12:00:00+03:00", JsonPath.read(body, "$.activities[0].end"));
    assertEquals(7200, jdbc.queryForObject("select start_offset_seconds from event_authoring.activity", Integer.class));
    assertEquals(10800, jdbc.queryForObject("select end_offset_seconds from event_authoring.activity", Integer.class));
    mvc.perform(get(BASE + "/" + event).with(user("session-user")))
        .andExpect(status().isOk()).andExpect(content().json(body));
    write("PUT", BASE + "/" + event + "/activities/" + activity, ACTIVITY.replace("Version\":0", "Version\":1").replace("Hands on", "Updated"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.activities[0].description").value("Updated"));
    write("PUT", BASE + "/" + event + "/activities/" + activity, ACTIVITY)
        .andExpect(status().isConflict());
    assertEquals(2L, jdbc.queryForObject("select version from event_authoring.event", Long.class));
  }
  @Test void US15_invalidActivityWritesRollBack() throws Exception {
    String path = BASE + "/" + event() + "/activities";
    for (String invalid : new String[] {
      ACTIVITY.replace("12:00:00+03:00", "10:00:00+02:00"),
      ACTIVITY.replace("25", "0"), ACTIVITY.replace("25", "1.5"), ACTIVITY.replace("25", "\"25\""),
      ACTIVITY.replace("\"capacity\":25,", ""),
      ACTIVITY.replace("\"expectedVersion\":0,", ""), ACTIVITY.replace("Version\":0", "Version\":0.5"),
      ACTIVITY.replace("Version\":0", "Version\":\"0\""), ACTIVITY.replace("Version\":0", "Version\":-1"),
      ACTIVITY.replace("\"location\":\"A\"", "\"location\":null"),
      ACTIVITY.replace("\"title\":\"Session\"", "\"title\":42"),
      ACTIVITY.replace("Session", "before\\u0000after"), ACTIVITY.replace("Public", "Private"),
      ACTIVITY.replace("\"registrationClose\":null", "\"registrationClose\":\"2026-12-01T11:00:00+02:00\""),
      ACTIVITY.replace("\"registrationOpen\":null", "\"registrationOpen\":\"2026-12-01T10:00:00+02:00\"")
    }) {
      write("POST", path, invalid).andExpect(status().isBadRequest());
      noActivities();
    }
  }
  @Test void US15_staffDeniedForeignNestedConcealedAndTerminalReadOnly() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    String other = event();
    write("PUT", BASE + "/" + other + "/activities/" + activity, ACTIVITY).andExpect(status().isNotFound());
    fixture.subject = "staff";
    fixture.organization("staff", "opaque-org", OrganizationAccessPort.Role.MEMBER);
    fixture.team("staff", event, EventTeamAccessPort.Role.STAFF);
    write("POST", BASE + "/" + event + "/activities", ACTIVITY.replace("Version\":0", "Version\":1")).andExpect(status().isForbidden());
    fixture.subject = "creator";
    for (String state : new String[] {"COMPLETED", "CANCELLED"}) {
      jdbc.update("update event_authoring.event set state=? where id=?::uuid", state, event);
      write("PUT", BASE + "/" + event + "/activities/" + activity, ACTIVITY.replace("Version\":0", "Version\":1")).andExpect(status().isConflict());
    }
  }
  @Test void EA03_draftRemovalSucceedsWithoutRegistrationAdapter() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    write("POST", BASE + "/" + event + "/activities/" + activity + "/agenda",
        "{\"expectedVersion\":1,\"time\":\"2026-12-01T10:30:00+02:00\",\"title\":\"Talk\"}")
        .andExpect(status().isCreated());
    write("DELETE", BASE + "/" + event + "/activities/" + activity + "?expectedVersion=2", "")
        .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("Draft"))
        .andExpect(jsonPath("$.version").value(3)).andExpect(jsonPath("$.activities").isEmpty());
    assertEquals(0, jdbc.queryForObject("select count(*) from event_authoring.activity", Integer.class));
    assertEquals(0, jdbc.queryForObject("select count(*) from event_authoring.agenda_item", Integer.class));
    assertEquals(3L, jdbc.queryForObject("select version from event_authoring.event", Long.class));
  }
  @Test void activityOpenApiDescribesTypedViewsAndRequiredVersion() throws Exception {
    mvc.perform(get("/v3/api-docs").with(user("session-user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/event-authoring/events/{eventId}/activities'].post.responses['201']").exists())
        .andExpect(jsonPath("$.components.schemas.ActivityWrite.required", org.hamcrest.Matchers.hasItems("capacity", "expectedVersion")))
        .andExpect(jsonPath("$.components.schemas.ActivityWrite.properties.start.format").value("date-time"))
        .andExpect(jsonPath("$.components.schemas.EventView.properties.activities.items['$ref']").value("#/components/schemas/ActivityView"));
  }

  @Test void activityTimestampsRequireRfc3339OffsetStringsAndOptionalTextRejectsNul() throws Exception {
    String path = BASE + "/" + event() + "/activities";
    for (String invalid : new String[] {
      ACTIVITY.replace("\"start\":\"2026-12-01T10:00:00+02:00\"", "\"start\":1796112000"),
      ACTIVITY.replace("2026-12-01T10:00:00+02:00", "2026-12-01T10:00:00"),
      ACTIVITY.replace("2026-12-01T10:00:00+02:00", "2026-12-01T10:00+02:00"),
      ACTIVITY.replace("\"registrationOpen\":null", "\"registrationOpen\":1796112000"),
      ACTIVITY.replace("\"registrationClose\":null", "\"registrationClose\":\"2026-12-01T08:00:00\""),
      ACTIVITY.replace("\"location\":\"A\"", "\"location\":\"before\\u0000after\""),
      ACTIVITY.replace("\"onlineLink\":null", "\"onlineLink\":\"before\\u0000after\"")
    }) {
      write("POST", path, invalid).andExpect(status().isBadRequest());
      noActivities();
    }
  }

  static String agenda(long version, String time, String title, String speaker) {
    return "{\"expectedVersion\":" + version + ",\"time\":\"" + time
        + "\",\"title\":\"" + title + "\",\"speaker\":" + (speaker == null ? "null" : "\"" + speaker + "\"") + "}";
  }
  String agendaPath(String event, String activity) { return BASE + "/" + event + "/activities/" + activity + "/agenda"; }
  String response(ResultActions action) throws Exception { return action.andReturn().getResponse().getContentAsString(); }

  @Test void US16_agendaCrudIncludesEndpointsOffsetsOptionalSpeakerAndSortedReads() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    String path = agendaPath(event, activity);
    String end = "2026-12-01T12:00:00+03:00";
    String start = "2026-12-01T10:00:00+02:00";
    String first = response(write("POST", path, agenda(1, end, "Closing", null)).andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(2)).andExpect(jsonPath("$.activities[0].agenda[0].time").value(end)));
    String closing = JsonPath.read(first, "$.activities[0].agenda[0].id");
    String second = response(write("POST", path, agenda(2, start, "Opening", "Speaker")).andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(3)).andExpect(jsonPath("$.activities[0].agenda[0].title").value("Opening")));
    String opening = JsonPath.read(second, "$.activities[0].agenda[0].id");
    String third = response(write("POST", path, agenda(3, start, "Same instant", null)).andExpect(status().isCreated()));
    java.util.List<String> ids = JsonPath.read(third, "$.activities[0].agenda[*].id");
    java.util.List<String> firstTwo = new java.util.ArrayList<>(ids.subList(0, 2));
    var sorted = firstTwo.stream().map(UUID::fromString).sorted().map(UUID::toString).toList();
    assertEquals(sorted, firstTwo);
    assertEquals(closing, ids.getLast());
    String updated = response(write("PUT", path + "/" + opening, agenda(4, start, "New opening", null))
        .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(5)));
    mvc.perform(get(BASE + "/" + event).with(user("session-user"))).andExpect(status().isOk()).andExpect(content().json(updated));
    write("DELETE", path + "/" + opening + "?expectedVersion=5", "").andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(6)).andExpect(jsonPath("$.activities[0].agenda.length()").value(2));
    write("DELETE", path + "/" + closing + "?expectedVersion=5", "").andExpect(status().isConflict());
    assertEquals(6L, jdbc.queryForObject("select version from event_authoring.event", Long.class));
    assertEquals(2, jdbc.queryForObject("select count(*) from event_authoring.agenda_item", Integer.class));
    assertEquals(10800, jdbc.queryForObject("select offset_seconds from event_authoring.agenda_item where id=?::uuid", Integer.class, closing));
  }

  @Test void US16_invalidAgendaInputNeverWritesOrBumpsVersion() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    String valid = agenda(1, "2026-12-01T10:00:00+02:00", "Opening", null);
    for (String invalid : new String[] {
      valid.replace("2026-12-01T10:00:00+02:00", "2026-12-01T09:59:59+02:00"),
      valid.replace("2026-12-01T10:00:00+02:00", "2026-12-01T12:00:01+03:00"),
      valid.replace("Opening", " "), valid.replace("Opening", "before\\u0000after"),
      valid.replace("\"title\":\"Opening\"", "\"title\":42"),
      valid.replace("\"time\":\"2026-12-01T10:00:00+02:00\",", ""),
      valid.replace("\"time\":\"2026-12-01T10:00:00+02:00\"", "\"time\":42"),
      valid.replace("2026-12-01T10:00:00+02:00", "2026-12-01T10:00:00"),
      valid.replace("\"speaker\":null", "\"speaker\":true"),
      valid.replace("\"speaker\":null", "\"speaker\":\"before\\u0000after\""),
      valid.replace("\"expectedVersion\":1,", ""), valid.replace("Version\":1", "Version\":1.5"),
      valid.replace("Version\":1", "Version\":\"1\"")
    }) {
      write("POST", agendaPath(event, activity), invalid).andExpect(status().isBadRequest());
      assertEquals(0, jdbc.queryForObject("select count(*) from event_authoring.agenda_item", Integer.class));
      assertEquals(1L, jdbc.queryForObject("select version from event_authoring.event", Long.class));
    }
  }

  @Test void US16_foreignEventActivityAndItemSubstitutionIsConcealed() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    String body = response(write("POST", agendaPath(event, activity), agenda(1, "2026-12-01T10:00:00+02:00", "Opening", null)).andExpect(status().isCreated()));
    String item = JsonPath.read(body, "$.activities[0].agenda[0].id");
    String other = event();
    String otherActivity = activityId(add(other));
    String valid = agenda(1, "2026-12-01T10:00:00+02:00", "Intrusion", null);
    write("POST", agendaPath(other, activity), valid).andExpect(status().isNotFound());
    write("PUT", agendaPath(other, otherActivity) + "/" + item, valid).andExpect(status().isNotFound());
    write("DELETE", agendaPath(other, otherActivity) + "/" + item + "?expectedVersion=1", "").andExpect(status().isNotFound());
    write("PUT", agendaPath(event, activity) + "/" + UUID.randomUUID(), valid.replace("Version\":1", "Version\":2")).andExpect(status().isNotFound());
    assertEquals(1, jdbc.queryForObject("select count(*) from event_authoring.agenda_item", Integer.class));
  }

  @Test void US16_scheduleReplacementCannotStrandAgendaOrExplicitClose() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    String before = response(write("POST", agendaPath(event, activity), agenda(1, "2026-12-01T10:00:00+02:00", "Opening", null)).andExpect(status().isCreated()));
    String path = BASE + "/" + event + "/activities/" + activity;
    write("PUT", path, ACTIVITY.replace("Version\":0", "Version\":2").replace("10:00:00+02:00", "10:30:00+02:00")).andExpect(status().isBadRequest());
    mvc.perform(get(BASE + "/" + event).with(user("session-user"))).andExpect(content().json(before));
    String explicit = ACTIVITY.replace("Version\":0", "Version\":2")
        .replace("\"registrationClose\":null", "\"registrationClose\":\"2026-12-01T09:30:00+02:00\"");
    String after = response(write("PUT", path, explicit).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3)));
    write("PUT", path, explicit.replace("Version\":2", "Version\":3").replace("10:00:00+02:00", "09:00:00+02:00"))
        .andExpect(status().isBadRequest());
    mvc.perform(get(BASE + "/" + event).with(user("session-user"))).andExpect(content().json(after));
  }

  @Test void US16_staffAndTerminalEventsCannotMutateAgenda() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    String path = agendaPath(event, activity);
    String body = response(write("POST", path, agenda(1, "2026-12-01T10:00:00+02:00", "Opening", null)).andExpect(status().isCreated()));
    String item = JsonPath.read(body, "$.activities[0].agenda[0].id");
    fixture.subject = "staff";
    fixture.organization("staff", "opaque-org", OrganizationAccessPort.Role.MEMBER);
    fixture.team("staff", event, EventTeamAccessPort.Role.STAFF);
    write("POST", path, agenda(2, "2026-12-01T10:00:00+02:00", "Denied", null)).andExpect(status().isForbidden());
    write("PUT", path + "/" + item, agenda(2, "2026-12-01T10:00:00+02:00", "Denied", null)).andExpect(status().isForbidden());
    write("DELETE", path + "/" + item + "?expectedVersion=2", "").andExpect(status().isForbidden());
    fixture.subject = "creator";
    for (String state : new String[] {"COMPLETED", "CANCELLED"}) {
      jdbc.update("update event_authoring.event set state=? where id=?::uuid", state, event);
      write("POST", path, agenda(2, "2026-12-01T10:00:00+02:00", "Denied", null)).andExpect(status().isConflict());
      write("DELETE", path + "/" + item + "?expectedVersion=2", "").andExpect(status().isConflict());
    }
    assertEquals(2L, jdbc.queryForObject("select version from event_authoring.event", Long.class));
  }

  @Test void nestedDeletesRequireExplicitIntegerVersionAndSafeErrorEnvelope() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    String path = agendaPath(event, activity);
    String body = response(write("POST", path, agenda(1, "2026-12-01T10:00:00+02:00", "Opening", null)).andExpect(status().isCreated()));
    String item = JsonPath.read(body, "$.activities[0].agenda[0].id");
    for (String endpoint : new String[] {BASE + "/" + event + "/activities/" + activity, path + "/" + item}) {
      for (String query : new String[] {"", "?expectedVersion=", "?expectedVersion=2.5", "?expectedVersion=-1", "?expectedVersion=true", "?expectedVersion=9223372036854775808"}) {
        write("DELETE", endpoint + query, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        assertEquals(2L, jdbc.queryForObject("select version from event_authoring.event", Long.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from event_authoring.agenda_item", Integer.class));
      }
    }
  }
  @Test void agendaOpenApiIncludesTypedItemsAndIntegerDeleteVersion() throws Exception {
    mvc.perform(get("/v3/api-docs").with(user("session-user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/event-authoring/events/{eventId}/activities/{activityId}/agenda'].post.responses['201']").exists())
        .andExpect(jsonPath("$.components.schemas.AgendaWrite.required", org.hamcrest.Matchers.hasItems("expectedVersion", "time", "title")))
        .andExpect(jsonPath("$.components.schemas.ActivityView.properties.agenda.items['$ref']").value("#/components/schemas/AgendaView"))
        .andExpect(jsonPath("$.paths['/api/event-authoring/events/{eventId}/activities/{activityId}'].delete.parameters[?(@.name == 'expectedVersion')].schema.type", org.hamcrest.Matchers.contains("integer")))
        .andExpect(jsonPath("$.paths['/api/event-authoring/events/{eventId}/activities/{activityId}/agenda/{itemId}'].delete.parameters[?(@.name == 'expectedVersion')].schema.type", org.hamcrest.Matchers.contains("integer")));
  }

  @Test void timestampPrecisionIsStableAndUnrepresentableIntervalReturnsSafe400() throws Exception {
    String event = event();
    String path = BASE + "/" + event + "/activities";
    String collapse = ACTIVITY.replace("2026-12-01T10:00:00+02:00", "2026-12-01T10:00:00.000000001+02:00")
        .replace("2026-12-01T12:00:00+03:00", "2026-12-01T10:00:00.000000002+02:00");
    assertDoesNotThrow(() -> write("POST", path, collapse)).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    noActivities();
    String precise = ACTIVITY.replace("2026-12-01T10:00:00+02:00", "2026-12-01T10:00:00.123456789+02:00");
    String created = response(write("POST", path, precise).andExpect(status().isCreated())
        .andExpect(jsonPath("$.activities[0].start").value("2026-12-01T10:00:00.123456+02:00")));
    mvc.perform(get(BASE + "/" + event).with(user("session-user"))).andExpect(content().json(created));
  }

  @Test void mutationsUseCurrentEditorAdminOwnerAndMembershipAuthority() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    fixture.subject = "editor";
    fixture.organization("editor", "opaque-org", OrganizationAccessPort.Role.MEMBER);
    fixture.team("editor", event, EventTeamAccessPort.Role.EDITOR);
    write("POST", agendaPath(event, activity), agenda(1, "2026-12-01T10:00:00+02:00", "Editor talk", null))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(2));
    fixture.subject = "admin";
    fixture.organization("admin", "opaque-org", OrganizationAccessPort.Role.ORG_ADMIN);
    write("PUT", BASE + "/" + event + "/activities/" + activity, ACTIVITY.replace("Version\":0", "Version\":2"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3));
    fixture.subject = "creator";
    fixture.organization("creator", "opaque-org", OrganizationAccessPort.Role.NONE);
    write("POST", agendaPath(event, activity), agenda(3, "2026-12-01T10:00:00+02:00", "Former member", null)).andExpect(status().isNotFound());
    fixture.organization("creator", "opaque-org", OrganizationAccessPort.Role.ORGANIZER);
    jdbc.update("update event_authoring.event set owner_subject='new-owner' where id=?::uuid", event);
    write("POST", agendaPath(event, activity), agenda(3, "2026-12-01T10:00:00+02:00", "Former owner", null)).andExpect(status().isNotFound());
    fixture.subject = "new-owner";
    fixture.organization("new-owner", "opaque-org", OrganizationAccessPort.Role.MEMBER);
    write("POST", agendaPath(event, activity), agenda(3, "2026-12-01T10:00:00+02:00", "Current owner", null)).andExpect(status().isCreated());
  }

  @Test void foreignNestedResourcesStayConcealedBeforeStaleVersionOrStaffDenial() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    String ownAgenda = response(write("POST", agendaPath(event, activity),
        agenda(1, "2026-12-01T10:00:00+02:00", "Own talk", null))
        .andExpect(status().isCreated()));
    String ownItem = JsonPath.read(ownAgenda, "$.activities[0].agenda[0].id");
    String other = event();
    String foreignActivity = activityId(add(other));
    String foreignAgenda = response(write("POST", agendaPath(other, foreignActivity),
        agenda(1, "2026-12-01T10:00:00+02:00", "Foreign talk", null))
        .andExpect(status().isCreated()));
    String foreignItem = JsonPath.read(foreignAgenda, "$.activities[0].agenda[0].id");
    String foreignActivityPath = BASE + "/" + event + "/activities/" + foreignActivity;
    String ownAgendaPath = agendaPath(event, activity);
    String staleAgenda = agenda(0, "2026-12-01T10:00:00+02:00", "Attempt", null);
    for (String subject : new String[] {"creator", "staff"}) {
      fixture.subject = subject;
      if (subject.equals("staff")) {
        fixture.organization("staff", "opaque-org", OrganizationAccessPort.Role.MEMBER);
        fixture.team("staff", event, EventTeamAccessPort.Role.STAFF);
      }
      write("PUT", foreignActivityPath, ACTIVITY).andExpect(status().isNotFound());
      write("DELETE", foreignActivityPath + "?expectedVersion=0", "")
          .andExpect(status().isNotFound());
      write("POST", foreignActivityPath + "/agenda", staleAgenda)
          .andExpect(status().isNotFound());
      write("PUT", foreignActivityPath + "/agenda/" + foreignItem, staleAgenda)
          .andExpect(status().isNotFound());
      write("DELETE", foreignActivityPath + "/agenda/" + foreignItem + "?expectedVersion=0", "")
          .andExpect(status().isNotFound());
      write("PUT", ownAgendaPath + "/" + foreignItem, staleAgenda)
          .andExpect(status().isNotFound());
      write("DELETE", ownAgendaPath + "/" + foreignItem + "?expectedVersion=0", "")
          .andExpect(status().isNotFound());
    }
    write("PUT", BASE + "/" + event + "/activities/" + activity,
        ACTIVITY.replace("Version\":0", "Version\":2"))
        .andExpect(status().isForbidden());
    write("PUT", ownAgendaPath + "/" + ownItem,
        agenda(2, "2026-12-01T10:00:00+02:00", "Denied", null))
        .andExpect(status().isForbidden());
    assertEquals(2, jdbc.queryForObject("select count(*) from event_authoring.activity", Integer.class));
    assertEquals(2, jdbc.queryForObject("select count(*) from event_authoring.agenda_item", Integer.class));
    assertEquals(java.util.List.of(2L, 2L), jdbc.queryForList(
        "select version from event_authoring.event order by id", Long.class));
    assertEquals(java.util.List.of("Foreign talk", "Own talk"), jdbc.queryForList(
        "select title from event_authoring.agenda_item order by title", String.class));
  }

}
