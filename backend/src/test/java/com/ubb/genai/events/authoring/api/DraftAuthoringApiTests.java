package com.ubb.genai.events.authoring.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.ubb.genai.events.authoring.application.AuthoringException;
import com.ubb.genai.events.authoring.ports.*;
import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestConfiguration.class, DraftAuthoringApiTests.TrustedPorts.class})
class DraftAuthoringApiTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Fixture fixture;
  static final String BASE = "/api/event-authoring/events";
  static final String BODY =
      "{\"title\":\"Workshop\",\"description\":\"Details\",\"category\":\"Workshop\",\"organizationRef\":\"opaque-org\"}";

  static class Fixture {
    String subject = "creator";
    boolean eligible = true;
    boolean unavailable = false;
    int organizationLookups;

    record OrganizationKey(String subject, String organizationRef) {}

    record TeamKey(String subject, UUID eventId) {}

    final Map<OrganizationKey, OrganizationAccessPort.Role> organizations = new HashMap<>();
    final Map<TeamKey, EventTeamAccessPort.Role> teams = new HashMap<>();

    void organization(String subject, String organizationRef, OrganizationAccessPort.Role role) {
      organizations.put(new OrganizationKey(subject, organizationRef), role);
    }

    void team(String subject, String eventId, EventTeamAccessPort.Role role) {
      teams.put(new TeamKey(subject, UUID.fromString(eventId)), role);
    }
  }

  @TestConfiguration
  static class TrustedPorts {
    @Bean
    Fixture fixture() {
      return new Fixture();
    }

    @Bean
    AuthoringIdentityPort identity(Fixture f) {
      return () -> {
        if (f.unavailable) throw AuthoringException.unavailable();
        return f.subject == null ? null : new AuthoringIdentityPort.Actor(f.subject, f.eligible);
      };
    }

    @Bean
    OrganizationAccessPort organization(Fixture f) {
      return (s, o) -> {
        f.organizationLookups++;
        return f.organizations.getOrDefault(
            new Fixture.OrganizationKey(s, o), OrganizationAccessPort.Role.NONE);
      };
    }

    @Bean
    @org.springframework.context.annotation.Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2026-11-10T10:00:00.123456789Z"), ZoneOffset.UTC);
    }

    @Bean
    EventTeamAccessPort team(Fixture f) {
      return (s, e) ->
          f.teams.getOrDefault(new Fixture.TeamKey(s, e), EventTeamAccessPort.Role.NONE);
    }
  }

  @BeforeEach
  void reset() {
    fixture.subject = "creator";
    fixture.eligible = true;
    fixture.unavailable = false;
    fixture.organizationLookups = 0;
    fixture.organizations.clear();
    fixture.teams.clear();
    fixture.organization("creator", "opaque-org", OrganizationAccessPort.Role.ORGANIZER);
    jdbc.update("delete from event_authoring.agenda_item");
    jdbc.update("delete from event_authoring.activity");
    jdbc.update("delete from event_authoring.event");
  }

  ResultActions create(String body) throws Exception {
    return mvc.perform(
        post(BASE)
            .with(user("session-user"))
            .with(csrf())
            .contentType("application/json")
            .content(body));
  }

  String createId() throws Exception {
    return createId("opaque-org");
  }

  String createId(String organizationRef) throws Exception {
    var response =
        create(BODY.replace("opaque-org", organizationRef))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    return JsonPath.read(response.getContentAsString(), "$.id");
  }

  @Test
  void US14_createsAndReadsPersistedDraftWithRedaction() throws Exception {
    create(BODY)
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(BASE + "/")))
        .andExpect(jsonPath("$.state").value("Draft"))
        .andExpect(jsonPath("$.category").value("Workshop"))
        .andExpect(jsonPath("$.version").value(0))
        .andExpect(jsonPath("$.activities").isEmpty())
        .andExpect(jsonPath("$.ownerSubject").doesNotExist())
        .andExpect(jsonPath("$.actor").doesNotExist())
        .andExpect(jsonPath("$.email").doesNotExist());
    String id = jdbc.queryForObject("select id::text from event_authoring.event", String.class);
    assertEquals(
        "creator",
        jdbc.queryForObject("select owner_subject from event_authoring.event", String.class));
    mvc.perform(get(BASE + "/" + id).with(user("session-user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.capabilities.canEdit").value(true));
  }

  @Test
  void rejectsClientOwnerActorAndStateInjection() throws Exception {
    for (String field : new String[] {"ownerSubject", "owner", "actor", "state"})
      create(BODY.substring(0, BODY.length() - 1) + ",\"" + field + "\":\"attacker\"}")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    assertEquals(
        0, jdbc.queryForObject("select count(*) from event_authoring.event", Integer.class));
  }

  @Test
  void invalidCategoryAndRequiredFieldsReturnSafeEnvelope() throws Exception {
    for (String body :
        new String[] {
          BODY.replace("Workshop\",\"organizationRef", "Unknown\",\"organizationRef"),
          BODY.replace("Details", " "),
          BODY.replace("opaque-org", ""),
          BODY.replace("\"Workshop\",\"organizationRef", "null,\"organizationRef")
        })
      create(body)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
          .andExpect(jsonPath("$.violations").isArray())
          .andExpect(jsonPath("$.trace").doesNotExist());
  }

  static Stream<Arguments> malformedTextValues() {
    return Stream.of("title", "description", "organizationRef")
        .flatMap(
            field ->
                Stream.of("23", "true", "false", "1.5", "[]", "{}", "[\"text\"]")
                    .map(value -> Arguments.of(field, value)));
  }

  @ParameterizedTest
  @MethodSource("malformedTextValues")
  void nonTextMetadataIsRejectedWithoutPersistence(String field, String scalar) throws Exception {
    String original =
        switch (field) {
          case "title" -> "Workshop";
          case "description" -> "Details";
          default -> "opaque-org";
        };
    String body =
        BODY.replace("\"" + field + "\":\"" + original + "\"", "\"" + field + "\":" + scalar);
    create(body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.violations").isArray());
    assertEquals(
        0, jdbc.queryForObject("select count(*) from event_authoring.event", Integer.class));
  }

  @Test
  void anonymousCannotReadOrCreateAndCsrfRemainsDistinct() throws Exception {
    mvc.perform(post(BASE).with(csrf()).contentType("application/json").content(BODY))
        .andExpect(status().isUnauthorized());
    mvc.perform(get(BASE + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    mvc.perform(post(BASE).with(user("session-user")).contentType("application/json").content(BODY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_REJECTED"));
  }

  @Test
  void trustedIdentityIsRequiredBeyondSession() throws Exception {
    fixture.subject = null;
    create(BODY).andExpect(status().isUnauthorized());
    fixture.subject = "creator";
    fixture.eligible = false;
    create(BODY).andExpect(status().isUnauthorized());
    fixture.eligible = true;
    fixture.unavailable = true;
    create(BODY).andExpect(status().isServiceUnavailable());
  }

  @Test
  void staffReadsWithoutWriteCapabilitiesAndNonteamIsConcealed() throws Exception {
    String id = createId();
    fixture.subject = "staff";
    fixture.organization("staff", "opaque-org", OrganizationAccessPort.Role.MEMBER);
    fixture.team("staff", id, EventTeamAccessPort.Role.STAFF);
    mvc.perform(get(BASE + "/" + id).with(user("session-user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.capabilities.canEdit").value(false))
        .andExpect(jsonPath("$.capabilities.canPublish").value(false));
    create(BODY).andExpect(status().isForbidden());
    fixture.team("staff", id, EventTeamAccessPort.Role.NONE);
    String concealed =
        mvc.perform(get(BASE + "/" + id).with(user("session-user")))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String missing =
        mvc.perform(get(BASE + "/" + UUID.randomUUID()).with(user("session-user")))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(missing, concealed);
  }

  @ParameterizedTest
  @ValueSource(strings = {"title", "description", "organizationRef"})
  void embeddedNulIsRejectedBeforeOrganizationLookupAndPersistence(String field) throws Exception {
    String original =
        switch (field) {
          case "title" -> "Workshop";
          case "description" -> "Details";
          default -> "opaque-org";
        };
    String body =
        BODY.replace(
            "\"" + field + "\":\"" + original + "\"", "\"" + field + "\":\"before\\u0000after\"");
    assertDoesNotThrow(() -> create(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.violations[0].path").value(field))
        .andExpect(jsonPath("$.violations[0].code").value("INVALID"))
        .andExpect(jsonPath("$.trace").doesNotExist());
    assertEquals(0, fixture.organizationLookups);
    assertEquals(
        0, jdbc.queryForObject("select count(*) from event_authoring.event", Integer.class));
  }

  @Test
  void createdTimestampExactlyMatchesImmediateReadAtSubMicrosecondClockPrecision()
      throws Exception {
    var created = create(BODY).andExpect(status().isCreated()).andReturn().getResponse();
    String id = JsonPath.read(created.getContentAsString(), "$.id");
    String timestamp = JsonPath.read(created.getContentAsString(), "$.createdAt");
    assertEquals("2026-11-10T10:00:00.123456Z", timestamp);
    assertEquals(BASE + "/" + id, created.getHeader("Location"));
    mvc.perform(get(BASE + "/" + id).with(user("session-user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.createdAt").value(timestamp));
  }

  @Test
  void authorizationUsesActualSubjectOrganizationAndEventLookupKeys() throws Exception {
    fixture.organization("creator", "other-org", OrganizationAccessPort.Role.ORGANIZER);
    String staffed = createId();
    String unassigned = createId();
    String administered = createId("other-org");
    fixture.organization("reader", "opaque-org", OrganizationAccessPort.Role.MEMBER);
    fixture.organization("reader", "other-org", OrganizationAccessPort.Role.ORG_ADMIN);
    fixture.team("reader", staffed, EventTeamAccessPort.Role.STAFF);
    fixture.subject = "reader";
    mvc.perform(get(BASE + "/" + staffed).with(user("session-user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.capabilities.canEdit").value(false));
    mvc.perform(get(BASE + "/" + unassigned).with(user("session-user")))
        .andExpect(status().isNotFound());
    mvc.perform(get(BASE + "/" + administered).with(user("session-user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.capabilities.canEdit").value(true));
    create(BODY).andExpect(status().isForbidden());
    create(BODY.replace("opaque-org", "other-org")).andExpect(status().isCreated());
    fixture.organization("unrelated", "opaque-org", OrganizationAccessPort.Role.MEMBER);
    fixture.subject = "unrelated";
    mvc.perform(get(BASE + "/" + staffed).with(user("session-user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void unknownIdFormatReturns400() throws Exception {
    mvc.perform(get(BASE + "/invalid").with(user("session-user")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }
}
