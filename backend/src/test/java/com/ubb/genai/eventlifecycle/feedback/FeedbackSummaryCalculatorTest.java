package com.ubb.genai.eventlifecycle.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedbackSummaryCalculatorTest {

    private static final Instant T0 = Instant.parse("2027-04-01T12:00:00Z");
    private final UUID activityId = UUID.randomUUID();

    private Feedback feedback(int rating, String comment, long minutes) {
        return new Feedback(activityId, UUID.randomUUID(), rating, comment, T0.plusSeconds(minutes * 60));
    }

    @Test
    @DisplayName("US-48 the average and the 1 to 5 distribution are computed")
    void us48_averageAndDistribution() {
        FeedbackSummary summary = FeedbackSummaryCalculator.summarize(
                activityId, "Talk", List.of(feedback(5, null, 1), feedback(4, null, 2), feedback(4, null, 3), feedback(1, null, 4)));

        assertThat(summary.count()).isEqualTo(4);
        assertThat(summary.averageRating()).isEqualTo(3.5);
        assertThat(summary.distribution()).containsExactly(
                java.util.Map.entry(1, 1), java.util.Map.entry(2, 0), java.util.Map.entry(3, 0),
                java.util.Map.entry(4, 2), java.util.Map.entry(5, 1));
        assertThat(summary.activityId()).isEqualTo(activityId);
        assertThat(summary.activityTitle()).isEqualTo("Talk");
    }

    @Test
    @DisplayName("US-48 no feedback gives a null average and a zero-filled distribution")
    void us48_noFeedbackGivesNullAverageAndZeroDistribution() {
        FeedbackSummary summary = FeedbackSummaryCalculator.summarize(activityId, "Talk", List.of());

        assertThat(summary.count()).isZero();
        assertThat(summary.averageRating()).isNull();
        assertThat(summary.distribution()).containsExactly(
                java.util.Map.entry(1, 0), java.util.Map.entry(2, 0), java.util.Map.entry(3, 0),
                java.util.Map.entry(4, 0), java.util.Map.entry(5, 0));
        assertThat(summary.comments()).isEmpty();
    }

    @Test
    @DisplayName("US-48 the average is rounded half up to two decimals")
    void us48_averageIsRoundedHalfUp() {
        // (5 + 5 + 4 + 4 + 4 + 4 + 4 + 4) / 8 = 4.25 ; 1, 1, 2 -> 1.333.. ; 5, 5, 4 -> 4.666..
        assertThat(FeedbackSummaryCalculator.summarize(activityId, "T",
                List.of(feedback(1, null, 1), feedback(1, null, 2), feedback(2, null, 3))).averageRating()).isEqualTo(1.33);
        assertThat(FeedbackSummaryCalculator.summarize(activityId, "T",
                List.of(feedback(5, null, 1), feedback(5, null, 2), feedback(4, null, 3))).averageRating()).isEqualTo(4.67);
        assertThat(FeedbackSummaryCalculator.summarize(activityId, "T",
                List.of(feedback(1, null, 1), feedback(2, null, 2), feedback(2, null, 3), feedback(2, null, 4),
                        feedback(2, null, 5), feedback(2, null, 6), feedback(2, null, 7), feedback(2, null, 8)))
                .averageRating()).isEqualTo(1.88);
    }

    @Test
    @DisplayName("US-48 only non-null comments are listed, newest first, with their rating")
    void us48_commentsAreNewestFirstAndSkipEmptyOnes() {
        FeedbackSummary summary = FeedbackSummaryCalculator.summarize(activityId, "Talk", List.of(
                feedback(3, "old", 1), feedback(5, null, 2), feedback(2, "newest", 9), feedback(4, "middle", 5)));

        assertThat(summary.count()).isEqualTo(4);
        assertThat(summary.comments()).containsExactly(
                new FeedbackComment(2, "newest"), new FeedbackComment(4, "middle"), new FeedbackComment(3, "old"));
    }
}
