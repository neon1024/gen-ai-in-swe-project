package com.ubb.genai.eventlifecycle.feedback;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Anonymous aggregate of an activity's feedback (US-48). It never carries who wrote an entry (BR-17). */
public record FeedbackSummary(
        UUID activityId,
        String activityTitle,
        int count,
        Double averageRating,
        Map<Integer, Integer> distribution,
        List<FeedbackComment> comments) {
}
