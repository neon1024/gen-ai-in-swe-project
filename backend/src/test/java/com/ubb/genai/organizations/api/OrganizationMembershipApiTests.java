package com.ubb.genai.organizations.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class OrganizationMembershipApiTests {

    private static final UUID AI_SOCIETY = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID ROBOTICS_CLUB = UUID.fromString("10000000-0000-4000-8000-000000000002");
    private static final UUID LANGUAGE_EXCHANGE = UUID.fromString("10000000-0000-4000-8000-000000000003");
    private static final UUID AI_ADMIN = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID ROBOTICS_ADMIN = UUID.fromString("00000000-0000-4000-8000-000000000003");
    private static final UUID LANGUAGE_ADMIN = UUID.fromString("00000000-0000-4000-8000-000000000004");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    private final List<UUID> createdUsers = new ArrayList<>();
    private final List<UUID> createdEvents = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdEvents.forEach(id -> jdbc.update("delete from event_authoring.event where id = ?", id));
        createdUsers.forEach(id -> {
            jdbc.update("delete from organizations.join_request where applicant_id = ?", id);
            jdbc.update("delete from organizations.organization_membership where user_id = ?", id);
        });
    }

    @Test
    @DisplayName("US-06 prevents duplicate pending requests, supports withdrawal, and permits re-request")
    void joinRequestCanBeWithdrawnAndResubmitted() throws Exception {
        UUID applicant = createMemberlessUser();

        mvc.perform(post("/api/organizations/{organizationId}/join-requests", AI_SOCIETY)
                        .with(as(applicant)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicantId").value(applicant.toString()));

        mvc.perform(post("/api/organizations/{organizationId}/join-requests", AI_SOCIETY)
                        .with(as(applicant)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUEST_PENDING"));

        mvc.perform(delete("/api/organizations/{organizationId}/join-requests/me", AI_SOCIETY)
                        .with(as(applicant)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/organizations/{organizationId}/join-requests", AI_SOCIETY)
                        .with(as(applicant)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("US-07 accepts requests as Member and allows resubmission after rejection")
    void adminCanApproveOrRejectAndRejectedApplicantCanReapply() throws Exception {
        UUID applicant = createMemberlessUser();
        UUID approvalRequest = createRequest(applicant, AI_SOCIETY);

        mvc.perform(post("/api/organizations/{organizationId}/join-requests/{requestId}/approve",
                                AI_SOCIETY, approvalRequest)
                        .with(as(AI_ADMIN)))
                .andExpect(status().isNoContent());
        JsonNode approved = organizationFor(applicant, AI_SOCIETY);
        assertTrue(approved.get("member").asBoolean());
        assertEquals("MEMBER", approved.get("role").asText());
        assertEquals("APPROVED", approved.get("requestStatus").asText());

        UUID rejectedApplicant = createMemberlessUser();
        UUID rejectedRequest = createRequest(rejectedApplicant, AI_SOCIETY);
        mvc.perform(post("/api/organizations/{organizationId}/join-requests/{requestId}/reject",
                                AI_SOCIETY, rejectedRequest)
                        .with(as(AI_ADMIN)))
                .andExpect(status().isNoContent());
        createRequest(rejectedApplicant, AI_SOCIETY);
        assertEquals("PENDING", organizationFor(rejectedApplicant, AI_SOCIETY).get("requestStatus").asText());
    }

    @Test
    @DisplayName("US-08 updates roles and refuses demotion of the last Org Admin")
    void adminCanChangeRolesButCannotDemoteLastAdmin() throws Exception {
        UUID member = createMember(AI_SOCIETY, "MEMBER");

        mvc.perform(patch("/api/organizations/{organizationId}/members/{userId}", AI_SOCIETY, member)
                        .with(as(AI_ADMIN))
                        .contentType("application/json")
                        .content("{\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANIZER"));
        mvc.perform(patch("/api/organizations/{organizationId}/members/{userId}", AI_SOCIETY, member)
                        .with(as(AI_ADMIN))
                        .contentType("application/json")
                        .content("{\"role\":\"ORG_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORG_ADMIN"));

        mvc.perform(patch("/api/organizations/{organizationId}/members/{userId}", ROBOTICS_CLUB, ROBOTICS_ADMIN)
                        .with(as(ROBOTICS_ADMIN))
                        .contentType("application/json")
                        .content("{\"role\":\"MEMBER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ORG_ADMIN"));
    }

    @Test
    @DisplayName("US-09 removes event-team roles and refuses last-admin or active-owner removal")
    void removalCleansEventTeamRolesAndEnforcesOrganizationRules() throws Exception {
        UUID member = createMember(AI_SOCIETY, "ORGANIZER");
        UUID eventId = createEvent(AI_SOCIETY, AI_ADMIN, "DRAFT");
        createEventTeamAssignment(AI_SOCIETY, eventId, member);

        mvc.perform(delete("/api/organizations/{organizationId}/members/{userId}", AI_SOCIETY, member)
                        .with(as(AI_ADMIN)))
                .andExpect(status().isNoContent());
        assertEquals(0L, jdbc.queryForObject(
                "select count(*) from organizations.organization_membership where organization_id = ? and user_id = ?",
                Long.class, AI_SOCIETY, member));
        assertEquals(0L, jdbc.queryForObject(
                "select count(*) from organizations.event_team_membership where event_id = ? and user_id = ?",
                Long.class, eventId, member));

        UUID activeOwner = createMember(AI_SOCIETY, "MEMBER");
        createEvent(AI_SOCIETY, activeOwner, "PUBLISHED");
        mvc.perform(delete("/api/organizations/{organizationId}/members/{userId}", AI_SOCIETY, activeOwner)
                        .with(as(AI_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_EVENT_OWNER"));

        mvc.perform(delete("/api/organizations/{organizationId}/members/{userId}", ROBOTICS_CLUB, ROBOTICS_ADMIN)
                        .with(as(ROBOTICS_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ORG_ADMIN"));
    }

    @Test
    @DisplayName("US-10 removes event-team roles and refuses last-admin or active-owner departure")
    void leavingCleansEventTeamRolesAndEnforcesOrganizationRules() throws Exception {
        UUID member = createMember(LANGUAGE_EXCHANGE, "MEMBER");
        UUID eventId = createEvent(LANGUAGE_EXCHANGE, LANGUAGE_ADMIN, "DRAFT");
        createEventTeamAssignment(LANGUAGE_EXCHANGE, eventId, member);

        mvc.perform(delete("/api/organizations/{organizationId}/members/me", LANGUAGE_EXCHANGE)
                        .with(as(member)))
                .andExpect(status().isNoContent());
        assertEquals(0L, jdbc.queryForObject(
                "select count(*) from organizations.organization_membership where organization_id = ? and user_id = ?",
                Long.class, LANGUAGE_EXCHANGE, member));
        assertEquals(0L, jdbc.queryForObject(
                "select count(*) from organizations.event_team_membership where event_id = ? and user_id = ?",
                Long.class, eventId, member));

        UUID activeOwner = createMember(LANGUAGE_EXCHANGE, "MEMBER");
        createEvent(LANGUAGE_EXCHANGE, activeOwner, "DRAFT");
        mvc.perform(delete("/api/organizations/{organizationId}/members/me", LANGUAGE_EXCHANGE)
                        .with(as(activeOwner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_EVENT_OWNER"));

        mvc.perform(delete("/api/organizations/{organizationId}/members/me", ROBOTICS_CLUB)
                        .with(as(ROBOTICS_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ORG_ADMIN"));
    }

    @Test
    @DisplayName("Organization membership routes fail closed when no demo identity is supplied")
    void requiresAnIdentityForBrowse() throws Exception {
        mvc.perform(get("/api/organizations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        mvc.perform(get("/api/organizations/{organizationId}/members", AI_SOCIETY)
                        .with(as(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORG_ADMIN_REQUIRED"));
    }

    private UUID createMemberlessUser() {
        UUID userId = UUID.randomUUID();
        createdUsers.add(userId);
        return userId;
    }

    private UUID createMember(UUID organizationId, String role) {
        UUID userId = UUID.randomUUID();
        createdUsers.add(userId);
        jdbc.update("""
                insert into organizations.organization_membership
                    (id, organization_id, user_id, role, created_at)
                values (?, ?, ?, ?, ?)
                """, UUID.randomUUID(), organizationId, userId, role, Timestamp.from(Instant.now()));
        return userId;
    }

    private UUID createRequest(UUID applicant, UUID organizationId) throws Exception {
        MvcResult result = mvc.perform(post("/api/organizations/{organizationId}/join-requests", organizationId)
                        .with(as(applicant)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("id").asText());
    }

    private JsonNode organizationFor(UUID userId, UUID organizationId) throws Exception {
        JsonNode organizations = objectMapper.readTree(mvc.perform(get("/api/organizations")
                        .with(as(userId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
        for (JsonNode organization : organizations) {
            if (organizationId.toString().equals(organization.get("id").asText())) {
                return organization;
            }
        }
        throw new AssertionError("Organization not returned by the membership API.");
    }

    private UUID createEvent(UUID organizationId, UUID ownerId, String state) {
        UUID eventId = UUID.randomUUID();
        createdEvents.add(eventId);
        jdbc.update("""
                insert into event_authoring.event
                    (id, organization_ref, owner_subject, title, description, category, state, created_at, version)
                values (?, ?, ?, 'Membership rule test', 'Temporary integration event', 'OTHER', ?, now(), 0)
                """, eventId, organizationId.toString(), ownerId.toString(), state);
        return eventId;
    }

    private void createEventTeamAssignment(UUID organizationId, UUID eventId, UUID userId) {
        jdbc.update("""
                insert into organizations.event_team_membership
                    (id, organization_id, event_id, user_id, role)
                values (?, ?, ?, ?, 'STAFF')
                """, UUID.randomUUID(), organizationId, eventId, userId);
    }

    /** Session user (principal = user id, as at login) plus the CSRF token the SPA echoes. */
    private static RequestPostProcessor as(Object userId) {
        return request -> csrf().postProcessRequest(user(String.valueOf(userId)).postProcessRequest(request));
    }
}
