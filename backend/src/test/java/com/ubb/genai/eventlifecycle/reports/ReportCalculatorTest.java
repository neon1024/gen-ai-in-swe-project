package com.ubb.genai.eventlifecycle.reports;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubb.genai.eventlifecycle.shared.model.ActivityState;
import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.port.SignupFigures;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportCalculatorTest {

    private static final Instant START = Instant.parse("2027-04-01T10:00:00Z");

    private final LifecycleActivity activity =
            LifecycleActivity.scheduled("Talk", START, START.plusSeconds(3600), ActivityVisibility.PUBLIC);

    @Test
    @DisplayName("US-49 the attendance rate is the share of confirmed participants marked attended")
    void us49_attendanceRateIsShareOfConfirmedMarkedAttended() {
        ActivityReportRow row = ReportCalculator.row(activity, new SignupFigures(1, 2, 3, 3), 2, 4.0);

        assertThat(row.activityId()).isEqualTo(activity.getId());
        assertThat(row.title()).isEqualTo("Talk");
        assertThat(row.startsAt()).isEqualTo(START);
        assertThat(row.state()).isEqualTo(ActivityState.SCHEDULED);
        assertThat(row.confirmedSignups()).isEqualTo(4);
        assertThat(row.waitlistSize()).isEqualTo(2);
        assertThat(row.cancellations()).isEqualTo(3);
        assertThat(row.attendanceRate()).isEqualTo(0.75);
        assertThat(row.ratingCount()).isEqualTo(2);
        assertThat(row.averageRating()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("US-49 no confirmed participants gives a null rate")
    void us49_noConfirmedParticipantsGivesNullRate() {
        ActivityReportRow row = ReportCalculator.row(activity, new SignupFigures(0, 1, 2, 0), 0, null);

        assertThat(row.confirmedSignups()).isZero();
        assertThat(row.attendanceRate()).isNull();
        assertThat(row.averageRating()).isNull();
    }

    @Test
    @DisplayName("US-49 the average rating is rounded half up to two decimals")
    void us49_averageRatingIsRoundedHalfUp() {
        ActivityReportRow row = ReportCalculator.row(activity, new SignupFigures(0, 0, 0, 0), 3, 3.125);

        assertThat(row.averageRating()).isEqualTo(3.13);
    }
}
