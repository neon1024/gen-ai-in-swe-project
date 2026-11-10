package com.ubb.genai.eventlifecycle.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Renders an event report as RFC 4180 CSV, neutralising spreadsheet formulas in text cells (US-50). */
public final class ReportCsvWriter {

    private static final String EOL = "\r\n";
    private static final List<String> HEADER = List.of(
            "activity_title", "starts_at", "status", "confirmed_signups", "waitlist_size", "cancellations",
            "attendance_rate_percent", "average_rating", "rating_count");

    private ReportCsvWriter() {
    }

    public static String write(EventReport report) {
        StringBuilder csv = new StringBuilder(String.join(",", HEADER)).append(EOL);
        for (ActivityReportRow row : report.activities()) {
            csv.append(String.join(",",
                    text(row.title()),
                    row.startsAt() == null ? "" : row.startsAt().toString(),
                    row.state().name(),
                    String.valueOf(row.confirmedSignups()),
                    String.valueOf(row.waitlistSize()),
                    String.valueOf(row.cancellations()),
                    row.attendanceRate() == null ? "" : percent(row.attendanceRate()),
                    row.averageRating() == null ? "" : String.valueOf(row.averageRating()),
                    String.valueOf(row.ratingCount()))).append(EOL);
        }
        return csv.toString();
    }

    private static String percent(double fraction) {
        return BigDecimal.valueOf(fraction * 100).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private static String text(String value) {
        if (value == null) {
            return "";
        }
        String safe = !value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        return safe.matches("(?s).*[,\"\r\n].*") ? "\"" + safe.replace("\"", "\"\"") + "\"" : safe;
    }
}
