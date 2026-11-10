package com.ubb.genai.eventlifecycle.reports;

import com.ubb.genai.eventlifecycle.shared.model.EventState;
import java.util.List;
import java.util.UUID;

/** Per-activity figures of an event for its team (US-49). Aggregates only, no personal data. */
public record EventReport(UUID eventId, String eventTitle, EventState eventState, List<ActivityReportRow> activities) {
}
