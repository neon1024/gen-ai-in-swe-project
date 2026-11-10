package com.ubb.genai.eventlifecycle.feedback;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Builds the anonymous feedback summary of an activity (US-48, BR-17). */
public final class FeedbackSummaryCalculator {

    private FeedbackSummaryCalculator() {
    }

    public static FeedbackSummary summarize(UUID activityId, String title, List<Feedback> feedback) {
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            distribution.put(rating, 0);
        }
        feedback.forEach(f -> distribution.merge(f.getRating(), 1, Integer::sum));

        Double average = feedback.isEmpty()
                ? null
                : BigDecimal.valueOf(feedback.stream().mapToInt(Feedback::getRating).sum())
                        .divide(BigDecimal.valueOf(feedback.size()), 2, RoundingMode.HALF_UP)
                        .doubleValue();

        List<FeedbackComment> comments = feedback.stream()
                .filter(f -> f.getComment() != null)
                .sorted(Comparator.comparing(Feedback::getCreatedAt).reversed())
                .map(f -> new FeedbackComment(f.getRating(), f.getComment()))
                .toList();

        return new FeedbackSummary(activityId, title, feedback.size(), average, distribution, comments);
    }
}
