package com.ubb.genai.eventlifecycle.shared.persistence;

import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Published events with at least one scheduled activity ending after {@code now}, activities fetched. */
    @Query("""
            select distinct e from LifecycleEvent e join fetch e.activities
            where e.state = com.ubb.genai.eventlifecycle.shared.model.EventState.PUBLISHED
            and exists (select 1 from LifecycleActivity a where a.event = e
                and a.state = com.ubb.genai.eventlifecycle.shared.model.ActivityState.SCHEDULED
                and a.endsAt > :now)
            """)
    List<LifecycleEvent> findPublishedWithActivityEndingAfter(@Param("now") Instant now);
}
