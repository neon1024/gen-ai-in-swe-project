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
  @PostMapping("/{eventId}/activities")
  @ApiResponse(responseCode = "201", description = "Activity created",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventView.class)))
  public ResponseEntity<EventView> addActivity(@PathVariable UUID eventId,
      @RequestBody AuthoringRequests.ActivityWrite request) {
    return ResponseEntity.status(201).body(service.addActivity(eventId, activityCommand(request)));
  }

  @org.springframework.web.bind.annotation.PutMapping("/{eventId}/activities/{activityId}")
  public EventView updateActivity(@PathVariable UUID eventId, @PathVariable UUID activityId,
      @RequestBody AuthoringRequests.ActivityWrite request) {
    return service.updateActivity(eventId, activityId, activityCommand(request));
  }

  @org.springframework.web.bind.annotation.DeleteMapping("/{eventId}/activities/{activityId}")
  public EventView removeActivity(@PathVariable UUID eventId, @PathVariable UUID activityId,
      @org.springframework.web.bind.annotation.RequestParam String expectedVersion) {
    return service.removeActivity(eventId, activityId, version(expectedVersion));
  }

  private static long version(String value) {
    try {
      if (!value.matches("[0-9]+")) throw new IllegalArgumentException();
      return Long.parseLong(value);
    } catch (IllegalArgumentException exception) {
      throw new AuthoringException(400, "INVALID_INPUT", "Invalid version");
    }
  }

  private static EventAuthoringService.ActivityCommand activityCommand(AuthoringRequests.ActivityWrite r) {
    return new EventAuthoringService.ActivityCommand(r.title(), r.description(), date(r.start()),
        date(r.end()), r.location(), r.onlineLink(), r.capacity(), date(r.registrationOpen()),
        date(r.registrationClose()), r.visibility(), r.expectedVersion());
  }

  private static java.time.OffsetDateTime date(String value) {
    if (value == null) return null;
    try {
      if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}[Tt][0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?([Zz]|[+-][0-9]{2}:[0-9]{2})"))
        throw new java.time.format.DateTimeParseException("RFC3339 timestamp required", value, 0);
      return java.time.OffsetDateTime.parse(value);
    } catch (java.time.format.DateTimeParseException exception) {
      throw new AuthoringException(400, "INVALID_INPUT", "Offset timestamp is required");
    }
  }

}
