package com.ubb.genai.eventlifecycle.feedback;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    boolean existsByActivityIdAndAuthorId(UUID activityId, UUID authorId);

    List<Feedback> findByActivityIdOrderByCreatedAtDesc(UUID activityId);

    @Query("""
            select new com.ubb.genai.eventlifecycle.feedback.RatingAggregate(f.activityId, count(f), avg(f.rating))
            from Feedback f
            where f.activityId in :ids
            group by f.activityId
            """)
    List<RatingAggregate> aggregateByActivityIds(@Param("ids") Collection<UUID> ids);
}
