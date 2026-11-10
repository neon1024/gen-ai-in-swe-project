package com.ubb.genai.eventlifecycle.feedback;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    boolean existsByActivityIdAndAuthorId(UUID activityId, UUID authorId);

    List<Feedback> findByActivityIdOrderByCreatedAtDesc(UUID activityId);
}
