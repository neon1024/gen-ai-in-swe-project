package com.ubb.genai.eventlifecycle.reports;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubb.genai.eventlifecycle.shared.model.ActivityState;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportCsvWriterTest {

    private static final String HEADER = "activity_title,starts_at,status,confirmed_signups,waitlist_size,"
            + "cancellations,attendance_rate_percent,average_rating,rating_count";

    private static ActivityReportRow row(String title, Double rate, Double average) {
        return new ActivityReportRow(UUID.randomUUID(), title, Instant.parse("2027-04-01T10:00:00Z"),
                ActivityState.SCHEDULED, 3, 1, 2, rate, average, 4);
    }

    private static EventReport report(ActivityReportRow... rows) {
        return new EventReport(UUID.randomUUID(), "Event", EventState.PUBLISHED, List.of(rows));
    }

    @Test
    @DisplayName("US-50 the CSV has a header and one row per activity")
    void us50_csvHasOneRowPerActivity() {
        String csv = ReportCsvWriter.write(report(row("A", 0.5, 4.0), row("B", null, null), row("C", 1.0, 3.5)));

        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(4);
        assertThat(lines[0]).isEqualTo(HEADER);
        assertThat(lines[1]).isEqualTo("A,2027-04-01T10:00:00Z,SCHEDULED,3,1,2,50.0,4.0,4");
        assertThat(lines[2]).isEqualTo("B,2027-04-01T10:00:00Z,SCHEDULED,3,1,2,,,4");
        assertThat(csv).endsWith("\r\n");
    }

    @Test
    @DisplayName("US-50 the attendance percentage has one decimal with a decimal point")
    void us50_percentHasOneDecimal() {
        String csv = ReportCsvWriter.write(report(row("A", 2.0 / 3.0, null)));

        assertThat(csv).contains(",66.7,");
    }

    @Test
    @DisplayName("US-50 a missing start time gives an empty cell")
    void us50_missingStartGivesEmptyCell() {
        ActivityReportRow noStart = new ActivityReportRow(UUID.randomUUID(), "A", null,
                ActivityState.CANCELLED, 0, 0, 0, null, null, 0);

        assertThat(ReportCsvWriter.write(report(noStart))).contains("\r\nA,,CANCELLED,0,0,0,,,0\r\n");
    }

    @Test
    @DisplayName("US-50 the CSV quotes special characters and neutralises formulas")
    void us50_csvEscapesAndNeutralisesFormulas() {
        String csv = ReportCsvWriter.write(report(
                row("=SUM(A1)", null, null),
                row("Talk, \"AI\"\nDay", null, null),
                row("@cmd", null, null),
                row("+1", null, null),
                row("-2", null, null),
                row("=A,B", null, null)));

        assertThat(csv).contains("\r\n'=SUM(A1),");
        assertThat(csv).contains("\r\n\"Talk, \"\"AI\"\"\nDay\",");
        assertThat(csv).contains("\r\n'@cmd,").contains("\r\n'+1,").contains("\r\n'-2,");
        assertThat(csv).contains("\r\n\"'=A,B\",");
    }

    @Test
    @DisplayName("US-50 numeric cells are never prefixed")
    void us50_numericCellsAreNeverPrefixed() {
        ActivityReportRow negative = new ActivityReportRow(UUID.randomUUID(), "A", null,
                ActivityState.SCHEDULED, -1, 0, 0, null, null, 0);

        assertThat(ReportCsvWriter.write(report(negative))).contains("\r\nA,,SCHEDULED,-1,");
    }

    @Test
    @DisplayName("US-50 the CSV contains aggregates only")
    void us50_csvContainsAggregatesOnly() {
        String csv = ReportCsvWriter.write(report(row("A", 0.5, 4.0), row("B", null, null)));

        assertThat(csv).doesNotContainPattern("[0-9a-f]{8}-[0-9a-f]{4}");
    }
}
