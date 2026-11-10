package com.ubb.genai.events.authoring.api;

import com.ubb.genai.events.authoring.api.AuthoringViews.EventView;
import com.ubb.genai.events.authoring.application.AuthoringException;
import com.ubb.genai.events.authoring.application.EventAuthoringService;
import com.ubb.genai.events.authoring.domain.EventCategory;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    EventCategory category = category(request.category());
    var view =
        service.createDraft(
            new EventAuthoringService.CreateEventCommand(
                request.title(), request.description(), category, request.organizationRef()));
    return ResponseEntity.created(URI.create("/api/event-authoring/events/" + view.id()))
        .body(view);
  }

  private static EventCategory category(String value) {
    EventCategory category = null;
    if (value != null) {
      try {
        category = EventCategory.valueOf(value.toUpperCase(Locale.ROOT));
        if (!wire(category).equals(value)) throw new IllegalArgumentException();
      } catch (IllegalArgumentException ex) {
        throw new AuthoringException(
            400,
            "INVALID_INPUT",
            "Invalid request",
            java.util.List.of(
                new AuthoringException.Violation("category", "INVALID", "Unknown category")));
      }
    }
    return category;
  }

  private static String wire(EventCategory c) {
    return c.name().substring(0, 1) + c.name().substring(1).toLowerCase(Locale.ROOT);
  }

  @PutMapping("/{eventId}")
  @ApiResponse(responseCode = "200", description = "Event details updated",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventView.class)))
  public EventView editDetails(@PathVariable UUID eventId,
      @RequestBody AuthoringRequests.EventDetails request) {
    return service.editEventDetails(eventId, new EventAuthoringService.EventDetailsCommand(
        request.expectedVersion(), request.title(), request.description(), category(request.category())));
  }

  @PostMapping("/{eventId}/publish")
  @ApiResponse(responseCode = "200", description = "Event published",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventView.class)))
  @ApiResponse(responseCode = "422", description = "Accumulated publication readiness problems",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthoringExceptionHandler.ErrorView.class)))
  public EventView publish(@PathVariable UUID eventId,
      @RequestBody AuthoringRequests.VersionCommand request) {
    if (request.expectedVersion() == null || request.expectedVersion() < 0)
      throw new AuthoringException(400, "INVALID_INPUT", "Nonnegative version is required");
    return service.publish(eventId, request.expectedVersion());
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

  @PutMapping("/{eventId}/activities/{activityId}")
  public EventView updateActivity(@PathVariable UUID eventId, @PathVariable UUID activityId,
      @RequestBody AuthoringRequests.ActivityWrite request) {
    return service.updateActivity(eventId, activityId, activityCommand(request));
  }

  @DeleteMapping("/{eventId}/activities/{activityId}")
  public EventView removeActivity(@PathVariable UUID eventId, @PathVariable UUID activityId,
      @Parameter(schema = @Schema(type = "integer", format = "int64", minimum = "0"))
      @RequestParam String expectedVersion) {
    return service.removeActivity(eventId, activityId, version(expectedVersion));
  }

  @PostMapping("/{eventId}/activities/{activityId}/agenda")
  @ApiResponse(responseCode = "201", description = "Agenda item created",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventView.class)))
  public ResponseEntity<EventView> addAgenda(@PathVariable UUID eventId, @PathVariable UUID activityId,
      @RequestBody AuthoringRequests.AgendaWrite request) {
    return ResponseEntity.status(201).body(service.addAgenda(eventId, activityId, agendaCommand(request)));
  }

  @PutMapping("/{eventId}/activities/{activityId}/agenda/{itemId}")
  public EventView updateAgenda(@PathVariable UUID eventId, @PathVariable UUID activityId,
      @PathVariable UUID itemId, @RequestBody AuthoringRequests.AgendaWrite request) {
    return service.updateAgenda(eventId, activityId, itemId, agendaCommand(request));
  }

  @DeleteMapping("/{eventId}/activities/{activityId}/agenda/{itemId}")
  public EventView removeAgenda(@PathVariable UUID eventId, @PathVariable UUID activityId,
      @PathVariable UUID itemId, @Parameter(schema = @Schema(type = "integer", format = "int64", minimum = "0"))
      @RequestParam String expectedVersion) {
    return service.removeAgenda(eventId, activityId, itemId, version(expectedVersion));
  }

  private static EventAuthoringService.AgendaCommand agendaCommand(AuthoringRequests.AgendaWrite r) {
    return new EventAuthoringService.AgendaCommand(date(r.time()), r.title(), r.speaker(), r.expectedVersion());
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

  private static OffsetDateTime date(String value) {
    if (value == null) return null;
    try {
      if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}[Tt][0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?([Zz]|[+-][0-9]{2}:[0-9]{2})"))
        throw new DateTimeParseException("RFC3339 timestamp required", value, 0);
      return OffsetDateTime.parse(value);
    } catch (DateTimeParseException exception) {
      throw new AuthoringException(400, "INVALID_INPUT", "Offset timestamp is required");
    }
  }
}
