package com.ubb.genai.eventlifecycle.feedback;

/** Whether the caller may give feedback; {@code reason} is the refusal name when not eligible. */
public record FeedbackEligibilityView(boolean eligible, String reason) {
}
