package com.ubb.genai.events.authoring.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class AuthoringWiringTests {
  @Autowired MockMvc mvc;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

  @Test
  void unrelatedRoutesRetainBaselineAuthentication() throws Exception {
    mvc.perform(get("/api/lifecycle/events/00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void guestsCanDiscoverEvents() throws Exception {
    mvc.perform(get("/api/discovery/events")).andExpect(status().isOk());
  }

  @Test
  void sessionWithoutAnActiveAccountIsNotEligible() throws Exception {
    mvc.perform(
            post("/api/event-authoring/events")
                .with(user("boot-user"))
                .with(csrf())
                .contentType("application/json")
                .content(DraftAuthoringApiTests.BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void loginSessionAuthorsDraftForItsOrganization() throws Exception {
    MockHttpSession session =
        (MockHttpSession)
            mvc.perform(
                    post("/api/auth/login")
                        .contentType("application/json")
                        .content(
                            "{\"email\":\"organizer@example.invalid\",\"password\":\"DemoPass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("00000000-0000-4000-8000-000000000007"))
                .andReturn()
                .getRequest()
                .getSession(false);
    mvc.perform(
            post("/api/event-authoring/events")
                .session(session)
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"title\":\"Workshop\",\"description\":\"Details\",\"category\":\"Workshop\","
                        + "\"organizationRef\":\"10000000-0000-4000-8000-000000000001\"}"))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/organizations").session(session)).andExpect(status().isOk());
  }

  @Test
  void loginRefusalsExplainTheReason() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"organizer@example.invalid\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void loginIssuesAFreshSessionId() throws Exception {
    MockHttpSession planted = new MockHttpSession();
    String plantedId = planted.getId();
    var session =
        mvc.perform(
                post("/api/auth/login")
                    .session(planted)
                    .contentType("application/json")
                    .content(
                        "{\"email\":\"organizer@example.invalid\",\"password\":\"DemoPass123!\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
    org.assertj.core.api.Assertions.assertThat(session.getId()).isNotEqualTo(plantedId);
  }

  @Test
  void baselineRoutesRejectWritesWithoutTheCsrfToken() throws Exception {
    mvc.perform(
            post("/api/organizations/10000000-0000-4000-8000-000000000002/join-requests")
                .with(user("00000000-0000-4000-8000-000000000008")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_REJECTED"));
  }

  @Test
  void pendingAccountSessionCannotAuthor() throws Exception {
    jdbc.update(
        "insert into users (id, email, password_hash, username, display_name, status) values"
            + " ('00000000-0000-4000-8000-0000000000a1', 'pending@example.invalid', 'x', 'pending_user',"
            + " 'Pending', 'PENDING') on conflict do nothing");
    mvc.perform(
            post("/api/event-authoring/events")
                .with(user("00000000-0000-4000-8000-0000000000a1"))
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"title\":\"T\",\"description\":\"D\",\"category\":\"Workshop\","
                        + "\"organizationRef\":\"10000000-0000-4000-8000-000000000001\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void authoringDoesNotUseBasicAuthentication() throws Exception {
    mvc.perform(
            get("/api/event-authoring/events/00000000-0000-0000-0000-000000000000")
                .with(httpBasic("boot-user", "not-an-authoritative-identity")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void openApiDescribesOwnedRoutes() throws Exception {
    mvc.perform(get("/v3/api-docs").with(user("session-user")))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.paths['/api/event-authoring/events'].post.responses['201'].content['application/json'].schema['$ref']")
                .value("#/components/schemas/EventView"))
        .andExpect(
            jsonPath(
                    "$.paths['/api/event-authoring/events'].post.responses['201'].headers.Location.schema.type")
                .value("string"))
        .andExpect(
            jsonPath("$.paths['/api/event-authoring/events'].post.responses['200']").doesNotExist())
        .andExpect(jsonPath("$.paths['/api/event-authoring/events/{eventId}'].get").exists());
  }
}
