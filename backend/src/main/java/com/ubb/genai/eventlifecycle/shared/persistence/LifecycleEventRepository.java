package com.ubb.genai.eventlifecycle.shared.persistence;

import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/**
 * Provisional: owned by the event-authoring share (jose03pedro); replaced at reconciliation.
 */
public interface LifecycleEventRepository extends JpaRepository<LifecycleEvent, UUID> {

    @EntityGraph(attributePaths = "activities")
    Optional<LifecycleEvent> findWithActivitiesById(UUID id);

    /** Loads the event and bumps its version on commit, so concurrent writers conflict. */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Optional<LifecycleEvent> findLockedById(UUID id);

    @Query("select distinct e from LifecycleEvent e left join fetch e.activities where e.state = com.ubb.genai.eventlifecycle.shared.model.EventState.PUBLISHED")
    List<LifecycleEvent> findPublishedWithActivities();
}
