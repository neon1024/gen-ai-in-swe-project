package com.ubb.genai.eventlifecycle.feedback;

import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Decides whether a participant may give feedback on an activity (US-47, BR-16). */
public final class FeedbackEligibility {

    /** Feedback stays open for this long after the activity ends, inclusive. */
    static final Duration WINDOW = Duration.ofDays(7);

    private FeedbackEligibility() {
    }

    /** Returns the first refusal in {@link FeedbackRefusal} order, or empty when feedback is allowed. */
    public static Optional<FeedbackRefusal> check(
            LifecycleActivity activity, Instant now, boolean markedPresent, boolean alreadyGiven) {
        if (activity.isCancelled()) {
            return Optional.of(FeedbackRefusal.ACTIVITY_CANCELLED);
        }
        if (!activity.hasEnded(now)) {
            return Optional.of(FeedbackRefusal.FEEDBACK_NOT_OPEN);
        }
        if (now.isAfter(activity.getEndsAt().plus(WINDOW))) {
            return Optional.of(FeedbackRefusal.FEEDBACK_WINDOW_CLOSED);
        }
        if (!markedPresent) {
            return Optional.of(FeedbackRefusal.NOT_ATTENDED);
        }
        if (alreadyGiven) {
            return Optional.of(FeedbackRefusal.FEEDBACK_ALREADY_GIVEN);
        }
        return Optional.empty();
    }
}
