package com.ubb.genai.eventlifecycle.shared.port;

/** Signup counts of an activity. */
public record SignupFigures(int confirmed, int waitlisted, int cancelled, int attended) {
}
