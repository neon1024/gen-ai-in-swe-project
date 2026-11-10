package com.ubb.genai.eventlifecycle.feedback;

/** Reasons feedback is refused, in evaluation order. */
public enum FeedbackRefusal {
    ACTIVITY_CANCELLED,
    FEEDBACK_NOT_OPEN,
    FEEDBACK_WINDOW_CLOSED,
    NOT_ATTENDED,
    FEEDBACK_ALREADY_GIVEN
}
