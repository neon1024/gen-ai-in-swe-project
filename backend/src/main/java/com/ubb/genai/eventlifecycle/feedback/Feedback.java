package com.ubb.genai.eventlifecycle.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A participant's rating of an activity. The author is stored only to enforce one feedback per participant and
 * activity; it is never exposed (BR-17).
 */
@Entity
@Table(name = "lifecycle_feedback")
public class Feedback {

    @Id
    private UUID id;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    private short rating;

    @Column(length = 2000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Feedback() {
    }

    public Feedback(UUID activityId, UUID authorId, int rating, String comment, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.activityId = activityId;
        this.authorId = authorId;
        this.rating = (short) rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
