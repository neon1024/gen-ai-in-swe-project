package com.ubb.genai.eventlifecycle.shared.persistence;

import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provisional: owned by the event-authoring share (jose03pedro); replaced at reconciliation.
 */
public interface LifecycleActivityRepository extends JpaRepository<LifecycleActivity, UUID> {
}
