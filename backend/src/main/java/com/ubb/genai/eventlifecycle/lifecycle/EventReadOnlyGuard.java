package com.ubb.genai.eventlifecycle.lifecycle;

import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Public API for other shares: refuses edits to COMPLETED and CANCELLED events (BR-06).
 *
 * <p>The event-authoring share calls {@link #requireEditable(UUID)} before every edit. Attendance and feedback
 * deliberately skip this guard: marking attendance and submitting feedback remain allowed on read-only events
 * (BR-06 exception).
 */
@Component
public class EventReadOnlyGuard {

    private final LifecycleEventRepository events;

    public EventReadOnlyGuard(LifecycleEventRepository events) {
        this.events = events;
    }

    /**
     * @throws LifecycleProblem 404 {@code EVENT_NOT_FOUND} when the event is missing, 409 {@code EVENT_READ_ONLY}
     *     when it is COMPLETED or CANCELLED
     */
    public void requireEditable(UUID eventId) {
        LifecycleEvent event = events.findById(eventId).orElseThrow(() -> LifecycleProblem.notFound("EVENT_NOT_FOUND"));
        if (event.isReadOnly()) {
            throw LifecycleProblem.conflict("EVENT_READ_ONLY", "The event is " + event.getState() + " and read-only");
        }
    }
}
