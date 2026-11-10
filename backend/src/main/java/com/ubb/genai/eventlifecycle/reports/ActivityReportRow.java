package com.ubb.genai.eventlifecycle.reports;

import com.ubb.genai.eventlifecycle.shared.model.ActivityState;
import java.time.Instant;
import java.util.UUID;

/** Figures of one activity. {@code attendanceRate} is a fraction (0 to 1) or null when nobody was confirmed. */
public record ActivityReportRow(
        UUID activityId,
        String title,
        Instant startsAt,
        ActivityState state,
        int confirmedSignups,
        int waitlistSize,
        int cancellations,
        Double attendanceRate,
        Double averageRating,
        int ratingCount) {
}
