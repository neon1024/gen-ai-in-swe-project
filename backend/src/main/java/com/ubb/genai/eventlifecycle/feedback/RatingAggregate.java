package com.ubb.genai.eventlifecycle.feedback;

import java.util.UUID;

/** Number of ratings and their unrounded mean for one activity. */
public record RatingAggregate(UUID activityId, long count, Double average) {
}
