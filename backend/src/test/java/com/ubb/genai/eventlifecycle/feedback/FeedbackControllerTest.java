package com.ubb.genai.eventlifecycle.feedback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FeedbackService service;

    @MockitoBean
    private CurrentUserProvider currentUser;

    private final UUID userId = UUID.randomUUID();
    private final UUID activityId = UUID.randomUUID();

    @Test
    @DisplayName("US-47 a rating outside 1 to 5 returns 400")
    void us47_ratingOutsideOneToFiveReturns400() throws Exception {
        for (String json : new String[] {"{\"rating\":0}", "{\"rating\":6}", "{\"comment\":\"x\"}"}) {
            mvc.perform(post("/api/feedback/activities/{id}", activityId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("US-47 a comment longer than 2000 characters returns 400")
    void us47_commentLongerThan2000Returns400() throws Exception {
        mvc.perform(post("/api/feedback/activities/{id}", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"comment\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("US-47 valid feedback returns 201 with the new id")
    void us47_validFeedbackReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));
        when(service.give(any(), any(), any())).thenReturn(id);

        mvc.perform(post("/api/feedback/activities/{id}", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Great\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));

        verify(service).give(activityId, Optional.of(userId), new GiveFeedbackRequest(5, "Great"));
    }

    @Test
    @DisplayName("US-47 the eligibility endpoint returns the view")
    void us47_eligibilityEndpointReturnsView() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));
        when(service.eligibility(activityId, Optional.of(userId)))
                .thenReturn(new FeedbackEligibilityView(false, "NOT_ATTENDED"));

        mvc.perform(get("/api/feedback/activities/{id}/eligibility", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.reason").value("NOT_ATTENDED"));
    }
}
