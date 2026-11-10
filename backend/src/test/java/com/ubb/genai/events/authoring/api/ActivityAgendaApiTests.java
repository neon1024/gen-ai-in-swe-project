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
  @Test void US15_missingRegistrationGuardDeniesRemovalAndKeepsRows() throws Exception {
    String event = event();
    String activity = activityId(add(event));
    write("DELETE", BASE + "/" + event + "/activities/" + activity + "?expectedVersion=1", "").andExpect(status().isServiceUnavailable());
    assertEquals(1, jdbc.queryForObject("select count(*) from event_authoring.activity", Integer.class));
    assertEquals(1L, jdbc.queryForObject("select version from event_authoring.event", Long.class));
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

}
