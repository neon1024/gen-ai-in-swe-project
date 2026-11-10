package com.ubb.genai.eventlifecycle.reports;

import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.port.SignupFigures;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Builds a report row from signup figures and rating aggregates (US-49). */
public final class ReportCalculator {

    private ReportCalculator() {
    }

    public static ActivityReportRow row(LifecycleActivity activity, SignupFigures figures, long ratingCount, Double average) {
        int confirmedSignups = figures.confirmed() + figures.attended();
        Double attendanceRate = confirmedSignups == 0 ? null : (double) figures.attended() / confirmedSignups;
        Double rounded = average == null || ratingCount == 0
                ? null
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP).doubleValue();
        return new ActivityReportRow(
                activity.getId(),
                activity.getTitle(),
                activity.getStartsAt(),
                activity.getState(),
                confirmedSignups,
                figures.waitlisted(),
                figures.cancelled(),
                attendanceRate,
                rounded,
                (int) ratingCount);
    }
}
