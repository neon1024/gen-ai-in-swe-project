package com.ubb.genai.eventlifecycle.shared.persistence;

import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provisional: owned by the event-authoring share (jose03pedro); replaced at reconciliation.
 */
public interface LifecycleEventRepository extends JpaRepository<LifecycleEvent, UUID> {
}
