package com.ubb.genai.eventlifecycle.lifecycle;

import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lifecycle")
public class LifecycleController {

    private final EventCancellationService service;
    private final CurrentUserProvider currentUser;

    public LifecycleController(EventCancellationService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/events/{eventId}")
    public LifecycleEventView event(@PathVariable UUID eventId) {
        return service.view(eventId, currentUser.currentUserId());
    }

    @PostMapping("/events/{eventId}/cancel")
    public LifecycleEventView cancelEvent(@PathVariable UUID eventId) {
        return service.cancelEvent(eventId, currentUser.currentUserId());
    }

    @PostMapping("/activities/{activityId}/cancel")
    public LifecycleEventView cancelActivity(@PathVariable UUID activityId) {
        return service.cancelActivity(activityId, currentUser.currentUserId());
    }
}
