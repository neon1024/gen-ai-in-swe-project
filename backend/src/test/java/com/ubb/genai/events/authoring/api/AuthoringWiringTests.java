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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class AuthoringWiringTests {
  @Autowired MockMvc mvc;

  @Test
  void unrelatedRoutesRetainBaselineAuthentication() throws Exception {
    mvc.perform(get("/").accept("text/html")).andExpect(status().is3xxRedirection());
  }

  @Test
  void defaultIdentityDoesNotInferEligibilityFromSession() throws Exception {
    mvc.perform(
            post("/api/event-authoring/events")
                .with(user("boot-user"))
                .with(csrf())
                .contentType("application/json")
                .content(DraftAuthoringApiTests.BODY))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
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
        .andExpect(jsonPath("$.paths['/api/event-authoring/events'].post").exists())
        .andExpect(jsonPath("$.paths['/api/event-authoring/events/{eventId}'].get").exists());
  }
}
