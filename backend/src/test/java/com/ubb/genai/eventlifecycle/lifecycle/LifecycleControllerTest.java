package com.ubb.genai.eventlifecycle.lifecycle;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ubb.genai.eventlifecycle.shared.model.ActivityState;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(LifecycleController.class)
class LifecycleControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventCancellationService service;

    @MockitoBean
    private DraftDeletionService deletion;

    @MockitoBean
    private CurrentUserProvider currentUser;

    private final UUID userId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID activityId = UUID.randomUUID();

    private LifecycleEventView view(EventState state) {
        return new LifecycleEventView(
                eventId,
                "Sample event",
                state,
                List.of(new LifecycleEventView.ActivityView(
                        activityId,
                        "Activity 1",
                        Instant.parse("2027-02-01T10:00:00Z"),
                        Instant.parse("2027-02-01T11:00:00Z"),
                        ActivityState.CANCELLED)),
                new LifecycleEventView.Permissions(false, false));
    }

    @Test
    @DisplayName("US-21 the cancel event endpoint returns the event view")
    void us21_cancelEventEndpointReturnsView() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));
        when(service.cancelEvent(eventId, Optional.of(userId))).thenReturn(view(EventState.CANCELLED));

        mvc.perform(post("/api/lifecycle/events/{id}/cancel", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELLED"))
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.activities[0].state").value("CANCELLED"))
                .andExpect(jsonPath("$.permissions.canCancel").value(false));
    }

    @Test
    @DisplayName("US-20 the cancel activity endpoint returns the event view")
    void us20_cancelActivityEndpointReturnsView() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));
        when(service.cancelActivity(activityId, Optional.of(userId))).thenReturn(view(EventState.PUBLISHED));

        mvc.perform(post("/api/lifecycle/activities/{id}/cancel", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
    }

    @Test
    @DisplayName("US-21 the get event endpoint returns the event view")
    void us21_getEventEndpointReturnsView() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.empty());
        when(service.view(eventId, Optional.empty())).thenReturn(view(EventState.PUBLISHED));

        mvc.perform(get("/api/lifecycle/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sample event"));
    }

    @Test
    @DisplayName("US-20 a conflict is mapped to a problem response with its code")
    void us20_cancelActivityEndpointMapsConflictToProblem() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));
        when(service.cancelActivity(activityId, Optional.of(userId)))
                .thenThrow(LifecycleProblem.conflict("ACTIVITY_ENDED", "Activity has ended"));

        mvc.perform(post("/api/lifecycle/activities/{id}/cancel", activityId))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ACTIVITY_ENDED"));
    }

    @Test
    @DisplayName("US-21 cancelling without a user returns 401")
    void us21_cancelWithoutUserReturns401() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.empty());
        when(service.cancelEvent(eventId, Optional.empty())).thenThrow(LifecycleProblem.unauthenticated());

        mvc.perform(post("/api/lifecycle/events/{id}/cancel", eventId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("US-20 a malformed activity id returns 400")
    void us20_malformedIdReturns400() throws Exception {
        mvc.perform(post("/api/lifecycle/activities/not-a-uuid/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("US-22 the delete endpoint returns 204")
    void us22_deleteEndpointReturns204() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));

        mvc.perform(delete("/api/lifecycle/events/{id}", eventId)).andExpect(status().isNoContent());

        verify(deletion).deleteDraft(eventId, Optional.of(userId));
    }

    @Test
    @DisplayName("US-22 deleting a published event maps to a 409 problem")
    void us22_deleteEndpointMapsConflict() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));
        org.mockito.Mockito.doThrow(LifecycleProblem.conflict("EVENT_NOT_DRAFT", "Published events cannot be deleted, only cancelled."))
                .when(deletion).deleteDraft(eventId, Optional.of(userId));

        mvc.perform(delete("/api/lifecycle/events/{id}", eventId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_DRAFT"));
    }
}
