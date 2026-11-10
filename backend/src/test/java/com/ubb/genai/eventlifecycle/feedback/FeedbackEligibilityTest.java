package com.ubb.genai.eventlifecycle.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedbackEligibilityTest {

    private static final Instant STARTS = Instant.parse("2027-02-01T10:00:00Z");
    private static final Instant ENDS = Instant.parse("2027-02-01T12:00:00Z");

    private final LifecycleActivity activity =
            LifecycleActivity.scheduled("Talk", STARTS, ENDS, ActivityVisibility.PUBLIC);

    private Optional<FeedbackRefusal> check(Instant now) {
        return FeedbackEligibility.check(activity, now, true, false);
    }

    @Test
    @DisplayName("BR-16 feedback is open from the end of the activity until seven days later, inclusive")
    void br16_feedbackWindowBoundaries() {
        assertThat(check(ENDS.minusSeconds(1))).contains(FeedbackRefusal.FEEDBACK_NOT_OPEN);
        assertThat(check(ENDS)).isEmpty();
        assertThat(check(ENDS.plus(Duration.ofDays(7)))).isEmpty();
        assertThat(check(ENDS.plus(Duration.ofDays(7)).plusSeconds(1))).contains(FeedbackRefusal.FEEDBACK_WINDOW_CLOSED);
    }

    @Test
    @DisplayName("US-47 a participant who was not marked present is refused")
    void us47_participantNotMarkedPresentIsRefused() {
        assertThat(FeedbackEligibility.check(activity, ENDS, false, false)).contains(FeedbackRefusal.NOT_ATTENDED);
    }

    @Test
    @DisplayName("US-47 a second feedback for the same activity is refused")
    void us47_secondFeedbackIsRefused() {
        assertThat(FeedbackEligibility.check(activity, ENDS, true, true)).contains(FeedbackRefusal.FEEDBACK_ALREADY_GIVEN);
    }

    @Test
    @DisplayName("US-47 a cancelled activity refuses feedback")
    void us47_cancelledActivityRefusesFeedback() {
        activity.cancel(STARTS.minusSeconds(60));

        assertThat(FeedbackEligibility.check(activity, ENDS, true, false)).contains(FeedbackRefusal.ACTIVITY_CANCELLED);
    }
}
