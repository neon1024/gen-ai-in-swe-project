package com.ubb.genai.events.authoring.api;

import com.ubb.genai.events.authoring.api.AuthoringViews.EventView;
import com.ubb.genai.events.authoring.application.AuthoringException;
import com.ubb.genai.events.authoring.application.EventAuthoringService;
import com.ubb.genai.events.authoring.domain.EventCategory;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-authoring/events")
public class EventAuthoringController {
  private final EventAuthoringService service;

  public EventAuthoringController(EventAuthoringService service) {
    this.service = service;
  }

  @PostMapping
  @ApiResponse(
      responseCode = "201",
      description = "Draft created",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = EventView.class)),
      headers =
          @Header(
              name = "Location",
              required = true,
              description = "Authoring detail URL",
              schema = @Schema(implementation = String.class, format = "uri-reference")))
  public ResponseEntity<EventView> create(@RequestBody AuthoringRequests.CreateEvent request) {
    EventCategory category = null;
    if (request.category() != null) {
      try {
        category = EventCategory.valueOf(request.category().toUpperCase(Locale.ROOT));
        if (!wire(category).equals(request.category())) throw new IllegalArgumentException();
      } catch (IllegalArgumentException ex) {
        throw new AuthoringException(
            400,
            "INVALID_INPUT",
            "Invalid request",
            java.util.List.of(
                new AuthoringException.Violation("category", "INVALID", "Unknown category")));
      }
    }
    var view =
        service.createDraft(
            new EventAuthoringService.CreateEventCommand(
                request.title(), request.description(), category, request.organizationRef()));
    return ResponseEntity.created(URI.create("/api/event-authoring/events/" + view.id()))
        .body(view);
  }

  private static String wire(EventCategory c) {
    return c.name().substring(0, 1) + c.name().substring(1).toLowerCase(Locale.ROOT);
  }

  @GetMapping("/{eventId}")
  public EventView get(@PathVariable UUID eventId) {
    return service.getEvent(eventId);
  }
}
