package com.ubb.genai.events.authoring.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Provisional X-User-Id identity (authoring.dev-stubs.enabled) plus the cookie CSRF the SPA uses. */
@SpringBootTest(properties = "authoring.dev-stubs.enabled=true")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class DevStubsApiTests {
  @Autowired MockMvc mvc;

  @Test
  void devUser_createsDraftAndGetsRefusedPublishWithCookieCsrf() throws Exception {
    String missing = "/api/event-authoring/events/00000000-0000-0000-0000-000000000000";
    Cookie xsrf = mvc.perform(get(missing).header("X-User-Id", "dev-user-1"))
        .andExpect(status().isNotFound())
        .andReturn().getResponse().getCookie("XSRF-TOKEN");
    org.junit.jupiter.api.Assertions.assertNotNull(xsrf);

    // Without the CSRF header the write is refused.
    mvc.perform(post("/api/event-authoring/events").header("X-User-Id", "dev-user-1").cookie(xsrf)
            .contentType("application/json").content(DraftAuthoringApiTests.BODY))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_REJECTED"));

    String created = mvc.perform(post("/api/event-authoring/events").header("X-User-Id", "dev-user-1")
            .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
            .contentType("application/json").content(DraftAuthoringApiTests.BODY))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("Draft"))
        .andReturn().getResponse().getContentAsString();
    String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

    mvc.perform(post("/api/event-authoring/events/" + id + "/publish").header("X-User-Id", "dev-user-1")
            .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
            .contentType("application/json").content("{\"expectedVersion\":0}"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void publishedEvent_appearsInDiscoveryAndFollowsEdits() throws Exception {
    String base = "/api/event-authoring/events";
    Cookie xsrf = mvc.perform(get(base + "/00000000-0000-0000-0000-000000000000").header("X-User-Id", "dev-user-2"))
        .andReturn().getResponse().getCookie("XSRF-TOKEN");
    var write = (java.util.function.Function<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder,
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder>) r ->
        r.header("X-User-Id", "dev-user-2").cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue()).contentType("application/json");
    String created = mvc.perform(write.apply(post(base)).content(
            DraftAuthoringApiTests.BODY.replace("\"title\":\"Workshop\"", "\"title\":\"Searchable zebra workshop\"")))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");
    String start = java.time.OffsetDateTime.now().plusDays(30).withNano(0).toString();
    String end = java.time.OffsetDateTime.now().plusDays(30).plusHours(2).withNano(0).toString();
    mvc.perform(write.apply(post(base + "/" + id + "/activities")).content(
            "{\"title\":\"Main\",\"description\":\"d\",\"start\":\"" + start + "\",\"end\":\"" + end
                + "\",\"location\":\"Room\",\"onlineLink\":null,\"capacity\":5,\"registrationOpen\":null,"
                + "\"registrationClose\":null,\"visibility\":\"Public\",\"expectedVersion\":0}"))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/discovery/events").param("q", "zebra"))
        .andExpect(jsonPath("$.totalItems").value(0)); // still a Draft: not searchable
    mvc.perform(write.apply(post(base + "/" + id + "/publish")).content("{\"expectedVersion\":1}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/discovery/events").param("q", "zebra"))
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.items[0].id").value(id));
    mvc.perform(write.apply(put(base + "/" + id)).content(
            "{\"expectedVersion\":2,\"title\":\"Renamed zebra event\",\"description\":\"x\",\"category\":\"Social\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/discovery/events").param("q", "Renamed zebra"))
        .andExpect(jsonPath("$.items[0].title").value("Renamed zebra event"));
  }

  @Test
  void withoutUserHeaderTheRequestIsUnauthenticated() throws Exception {
    mvc.perform(get("/api/event-authoring/events/00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isUnauthorized());
  }
}
