package com.ubb.genai.eventlifecycle.feedback;

/** A rating with its comment, without its author (BR-17). */
public record FeedbackComment(int rating, String comment) {
}
