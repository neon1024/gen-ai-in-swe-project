package com.ubb.genai.eventlifecycle.lifecycle;

import com.ubb.genai.eventlifecycle.shared.access.EventAccessPolicy;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deletes draft events (US-22); published, cancelled and completed events are never deleted (BR-05). */
@Service
public class DraftDeletionService {

    private final EventAccessPolicy access;
    private final LifecycleEventRepository events;

    public DraftDeletionService(EventAccessPolicy access, LifecycleEventRepository events) {
        this.access = access;
        this.events = events;
    }

    /** Deletes the draft with its activities and agenda items (cascaded by JPA and by the database). */
    @Transactional
    public void deleteDraft(UUID eventId, Optional<UUID> userId) {
        LifecycleEvent event = events.findById(eventId)
                .filter(e -> access.canSeeEvent(e, userId))
                .orElseThrow(() -> LifecycleProblem.notFound("EVENT_NOT_FOUND"));
        access.requireOwnerRights(event, access.requireUser(userId));
        if (event.getState() != EventState.DRAFT) {
            throw LifecycleProblem.conflict("EVENT_NOT_DRAFT", "Published events cannot be deleted, only cancelled.");
        }
        events.delete(event);
    }
}
