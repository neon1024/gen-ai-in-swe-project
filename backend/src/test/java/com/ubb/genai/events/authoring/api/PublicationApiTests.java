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

@SpringBootTest @AutoConfigureMockMvc
@Import({PostgresTestConfiguration.class,DraftAuthoringApiTests.TrustedPorts.class})
class PublicationApiTests {
  @Autowired MockMvc mvc;
  @Autowired EventAuthoringService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired DraftAuthoringApiTests.Fixture fixture;
  static final String BASE="/api/event-authoring/events/";
  @BeforeEach void reset() {
    fixture.subject="creator";fixture.eligible=true;fixture.unavailable=false;
    fixture.organizations.clear();fixture.teams.clear();
    fixture.organization("creator","org",OrganizationAccessPort.Role.ORGANIZER);
    jdbc.update("delete from event_authoring.agenda_item");jdbc.update("delete from event_authoring.activity");jdbc.update("delete from event_authoring.event");
  }
  UUID event(boolean activity) {
    var e=service.createDraft(new EventAuthoringService.CreateEventCommand("Event","Details",EventCategory.WORKSHOP,"org"));
    if(activity) service.addActivity(e.id(),new EventAuthoringService.ActivityCommand("Session","Details",OffsetDateTime.parse("2026-12-01T10:00:00+02:00"),OffsetDateTime.parse("2026-12-01T12:00:00+02:00"),"Room",null,10,null,null,null,0L));
    return e.id();
  }
  @Test void US17_emptyReadinessUses422Envelope() throws Exception {
    var id=event(false);
    mvc.perform(post(BASE+id+"/publish").with(user("session")).with(csrf()).contentType("application/json").content("{\"expectedVersion\":0}"))
        .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.violations[0].path").value("activities"));
    assertEquals("Draft",service.getEvent(id).state());
  }
  @Test void US17_publicationPersistsTimestampWindowsAndVersion() throws Exception {
    var id=event(true);
    var response=mvc.perform(post(BASE+id+"/publish").with(user("session")).with(csrf()).contentType("application/json").content("{\"expectedVersion\":1}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("Published"))
        .andExpect(jsonPath("$.version").value(2)).andExpect(jsonPath("$.publishedAt").value("2026-11-10T10:00:00.123456Z"))
        .andExpect(jsonPath("$.activities[0].effectiveRegistrationOpen").value("2026-11-10T10:00:00.123456Z"))
        .andExpect(jsonPath("$.ownerSubject").doesNotExist()).andReturn().getResponse().getContentAsString();
    mvc.perform(get(BASE+id).with(user("session"))).andExpect(status().isOk()).andExpect(content().json(response));
  }
  @Test void US17_publishVersionIsStrictMandatoryAndUnknownFieldsRejected() throws Exception {
    var id=event(true);
    for(var body:new String[]{"{}","{\"expectedVersion\":null}","{\"expectedVersion\":-1}","{\"expectedVersion\":\"1\"}","{\"expectedVersion\":1.0}","{\"expectedVersion\":true}","{\"expectedVersion\":1,\"state\":\"Published\"}"})
      mvc.perform(post(BASE+id+"/publish").with(user("session")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
  }
}
