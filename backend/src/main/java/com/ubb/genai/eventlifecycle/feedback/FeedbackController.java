package com.ubb.genai.eventlifecycle.feedback;

import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService service;
    private final CurrentUserProvider currentUser;

    public FeedbackController(FeedbackService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/activities/{activityId}/eligibility")
    public FeedbackEligibilityView eligibility(@PathVariable UUID activityId) {
        return service.eligibility(activityId, currentUser.currentUserId());
    }

    @PostMapping("/activities/{activityId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> give(@PathVariable UUID activityId, @Valid @RequestBody GiveFeedbackRequest request) {
        return Map.of("id", service.give(activityId, currentUser.currentUserId(), request));
    }
}
