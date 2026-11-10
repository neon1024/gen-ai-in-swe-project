package com.ubb.genai.eventlifecycle.reports;

import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final EventReportService service;
    private final CurrentUserProvider currentUser;

    public ReportController(EventReportService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/events/{eventId}")
    public EventReport report(@PathVariable UUID eventId) {
        return service.report(eventId, currentUser.currentUserId());
    }

    @GetMapping("/events/{eventId}/csv")
    public ResponseEntity<String> csv(@PathVariable UUID eventId) {
        String body = ReportCsvWriter.write(service.report(eventId, currentUser.currentUserId()));
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("event-" + eventId + "-report.csv").build().toString())
                .body(body);
    }
}
