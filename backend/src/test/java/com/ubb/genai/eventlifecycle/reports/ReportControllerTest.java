package com.ubb.genai.eventlifecycle.reports;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ubb.genai.eventlifecycle.shared.model.ActivityState;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventReportService service;

    @MockitoBean
    private CurrentUserProvider currentUser;

    private final UUID userId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    private EventReport report() {
        return new EventReport(eventId, "Conf", EventState.COMPLETED, List.of(new ActivityReportRow(
                UUID.randomUUID(), "Talk", Instant.parse("2027-04-01T10:00:00Z"), ActivityState.SCHEDULED,
                4, 2, 3, 0.75, 4.5, 2)));
    }

    @Test
    @DisplayName("US-49 the report endpoint returns the JSON report")
    void us49_reportEndpointReturnsJson() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));
        when(service.report(eventId, Optional.of(userId))).thenReturn(report());

        mvc.perform(get("/api/reports/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.eventState").value("COMPLETED"))
                .andExpect(jsonPath("$.activities[0].title").value("Talk"))
                .andExpect(jsonPath("$.activities[0].confirmedSignups").value(4))
                .andExpect(jsonPath("$.activities[0].attendanceRate").value(0.75))
                .andExpect(jsonPath("$.activities[0].averageRating").value(4.5))
                .andExpect(jsonPath("$.activities[0].ratingCount").value(2));
    }

    @Test
    @DisplayName("US-49 a refused caller gets the error status")
    void us49_refusedCallerGetsErrorStatus() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));
        when(service.report(eventId, Optional.of(userId))).thenThrow(LifecycleProblem.forbidden("TEAM_MEMBER_REQUIRED"));

        mvc.perform(get("/api/reports/events/{id}", eventId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TEAM_MEMBER_REQUIRED"));
    }

    @Test
    @DisplayName("US-50 the CSV endpoint downloads an attachment")
    void us50_csvEndpointDownloadsAttachment() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));
        when(service.report(eventId, Optional.of(userId))).thenReturn(report());

        mvc.perform(get("/api/reports/events/{id}/csv", eventId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"event-" + eventId + "-report.csv\""))
                .andExpect(content().string(ReportCsvWriter.write(report())));
    }
}
