package com.ubb.genai.events.authoring.application;

import com.ubb.genai.events.authoring.api.AuthoringViews.Capabilities;
import com.ubb.genai.events.authoring.api.AuthoringViews.EventView;
import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import com.ubb.genai.events.authoring.domain.EventCategory;
import com.ubb.genai.events.authoring.domain.EventState;
import com.ubb.genai.events.authoring.persistence.AuthoringEventRepository;
import com.ubb.genai.events.authoring.ports.AuthoringIdentityPort;
import com.ubb.genai.events.authoring.ports.EventTeamAccessPort;
import com.ubb.genai.events.authoring.ports.OrganizationAccessPort;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import com.ubb.genai.events.authoring.domain.*;
import com.ubb.genai.events.authoring.api.AuthoringViews.ActivityView;
import com.ubb.genai.events.authoring.ports.RegistrationAuthoringPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class EventAuthoringService {
  public record CreateEventCommand(
      String title, String description, EventCategory category, String organizationRef) {}

  private final AuthoringIdentityPort identity;
  private final OrganizationAccessPort organizations;
  private final EventTeamAccessPort team;
  private final AuthoringEventRepository repository;
  private final Clock clock;
  private final RegistrationAuthoringPort registration;

  public record ActivityCommand(String title, String description, OffsetDateTime start,
      OffsetDateTime end, String location, String onlineLink, Integer capacity,
      OffsetDateTime registrationOpen, OffsetDateTime registrationClose, String visibility,
      Long expectedVersion) {}


  public EventAuthoringService(
      AuthoringIdentityPort identity,
      OrganizationAccessPort organizations,
      EventTeamAccessPort team,
      AuthoringEventRepository repository,
      Clock clock) {
    this(identity, organizations, team, repository, clock, new RegistrationAuthoringPort() {
      public <T> T withLockedActivity(GuardRequest request,
          java.util.function.Function<RegistrationContext, T> mutation) {
        throw AuthoringException.unavailable();
      }
    });
  }

  public EventAuthoringService(AuthoringIdentityPort identity, OrganizationAccessPort organizations,
      EventTeamAccessPort team, AuthoringEventRepository repository, Clock clock,
      RegistrationAuthoringPort registration) {
    this.identity = identity;
    this.organizations = organizations;
    this.team = team;
    this.repository = repository;
    this.clock = clock;
    this.registration = registration;
  }

  @Transactional
  public EventView createDraft(CreateEventCommand command) {
    var actor = actor();
    var violations = new ArrayList<AuthoringException.Violation>();
    if (command == null) throw new AuthoringException(400, "INVALID_INPUT", "Invalid request");
    required(violations, "title", command.title());
    required(violations, "description", command.description());
    required(violations, "organizationRef", command.organizationRef());
    if (command.category() == null)
      violations.add(
          new AuthoringException.Violation("category", "REQUIRED", "Category is required"));
    if (!violations.isEmpty())
      throw new AuthoringException(400, "INVALID_INPUT", "Invalid request", violations);
    var role = organization(actor.subject(), command.organizationRef());
    if (role != OrganizationAccessPort.Role.ORGANIZER
        && role != OrganizationAccessPort.Role.ORG_ADMIN)
      throw new AuthoringException(403, "FORBIDDEN", "Action denied");
    var event =
        repository.save(
            AuthoringEvent.draft(
                command.organizationRef(),
                actor.subject(),
                command.title(),
                command.description(),
                command.category(),
                clock.instant()));
    return view(event, true);
  }

  @Transactional(readOnly = true)
  public EventView getEvent(UUID id) {
    var actor = actor();
    var event = repository.findById(id).orElseThrow(EventAuthoringService::notFound);
    return view(event, canEdit(actor, event));
  }

  private boolean canEdit(AuthoringIdentityPort.Actor actor, AuthoringEvent event) {
    var org = organization(actor.subject(), event.organizationRef());
    if (org == OrganizationAccessPort.Role.NONE) throw notFound();
    if (org == OrganizationAccessPort.Role.ORG_ADMIN || actor.subject().equals(event.ownerSubject())) return true;
    var role = team.roleFor(actor.subject(), event.id());
    if (role == null) throw AuthoringException.unavailable();
    if (role == EventTeamAccessPort.Role.NONE) throw notFound();
    return role == EventTeamAccessPort.Role.EDITOR;
  }

  private AuthoringEvent writable(UUID eventId, long expectedVersion) {
    var actor = actor();
    var event = repository.findForUpdate(eventId).orElseThrow(EventAuthoringService::notFound);
    if (!canEdit(actor, event)) throw new AuthoringException(403, "FORBIDDEN", "Action denied");
    if (event.state() != EventState.DRAFT && event.state() != EventState.PUBLISHED)
      throw new AuthoringException(409, "STATE_CONFLICT", "Event is read only");
    if (expectedVersion < 0) throw invalid("expectedVersion", "Version must be nonnegative");
    if (event.version() != expectedVersion)
      throw new AuthoringException(409, "VERSION_CONFLICT", "Version conflict");
    return event;
  }

  @Transactional
  public EventView addActivity(UUID eventId, ActivityCommand command) {
    checkCommand(command);
    var event = writable(eventId, command.expectedVersion());
    var activities = new ArrayList<>(event.activities());
    activities.add(activity(command, UUID.randomUUID(), clock.instant().truncatedTo(ChronoUnit.MICROS), false));
    return view(repository.replace(event.withActivities(activities)), true);
  }

  @Transactional
  public EventView updateActivity(UUID eventId, UUID activityId, ActivityCommand command) {
    checkCommand(command);
    var event = writable(eventId, command.expectedVersion());
    var previous = activity(event, activityId);
    var replacement = activity(command, activityId, previous.createdAt(), previous.cancelled());
    var activities = event.activities().stream().map(a -> a.id().equals(activityId) ? replacement : a).toList();
    return view(repository.replace(event.withActivities(activities)), true);
  }

  @Transactional
  public EventView removeActivity(UUID eventId, UUID activityId, long expectedVersion) {
    var event = writable(eventId, expectedVersion);
    activity(event, activityId);
    return registration.withLockedActivity(new RegistrationAuthoringPort.GuardRequest(eventId,
        activityId, RegistrationAuthoringPort.MutationKind.REMOVE), context -> {
      if (context == null) throw AuthoringException.unavailable();
      if (context.hasAnySignup()) throw new AuthoringException(409, "SIGNUP_CONFLICT", "Activity has signup history");
      var remaining = event.activities().stream().filter(a -> !a.id().equals(activityId)).toList();
      return view(repository.replace(event.withActivities(remaining)), true);
    });
  }

  private static AuthoringActivity activity(AuthoringEvent event, UUID id) {
    return event.activities().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow(EventAuthoringService::notFound);
  }

  private static void checkCommand(ActivityCommand command) {
    if (command == null) throw invalid("activity", "Activity is required");
    if (command.expectedVersion() == null || command.expectedVersion() < 0)
      throw invalid("expectedVersion", "Nonnegative version is required");
  }

  private static AuthoringActivity activity(ActivityCommand command, UUID id,
      java.time.Instant createdAt, boolean cancelled) {
    var violations = new ArrayList<AuthoringException.Violation>();
    required(violations, "title", command.title());
    required(violations, "description", command.description());
    optionalText(violations, "location", command.location());
    optionalText(violations, "onlineLink", command.onlineLink());
    if (!violations.isEmpty()) throw new AuthoringException(400, "INVALID_INPUT", "Invalid request", violations);
    if ((command.location() == null || command.location().isBlank())
        && (command.onlineLink() == null || command.onlineLink().isBlank()))
      throw invalid("location", "Location or online link is required");
    if (command.capacity() == null || command.capacity() < 1)
      throw invalid("capacity", "Capacity must be at least one");
    if (command.visibility() != null && !command.visibility().equals("Public"))
      throw invalid("visibility", "Only Public visibility is available");
    try {
      var schedule = new ActivitySchedule(command.start(), command.end());
      var window = new RegistrationWindow(command.registrationOpen(), command.registrationClose());
      window.validate(schedule);
      return new AuthoringActivity(id, command.title(), command.description(), schedule,
          command.location(), command.onlineLink(), command.capacity(), window,
          ActivityVisibility.PUBLIC, cancelled, createdAt);
    } catch (IllegalArgumentException exception) {
      throw invalid("schedule", exception.getMessage());
    }
  }

  private static void optionalText(List<AuthoringException.Violation> violations, String path, String value) {
    if (value != null && value.indexOf(0) >= 0)
      violations.add(new AuthoringException.Violation(path, "INVALID", "Value contains an unsupported character"));
  }

  private static AuthoringException invalid(String path, String message) {
    return new AuthoringException(400, "INVALID_INPUT", "Invalid request",
        List.of(new AuthoringException.Violation(path, "INVALID", message)));
  }

  private AuthoringIdentityPort.Actor actor() {
    var actor = identity.currentActor();
    if (actor == null || !actor.eligible() || actor.subject() == null || actor.subject().isBlank())
      throw new AuthoringException(401, "UNAUTHENTICATED", "Eligible authentication required");
    return actor;
  }

  private OrganizationAccessPort.Role organization(String subject, String org) {
    var role = organizations.forActor(subject, org);
    if (role == null) throw AuthoringException.unavailable();
    return role;
  }

  private static AuthoringException notFound() {
    return new AuthoringException(404, "NOT_FOUND", "Event not found");
  }

  private static void required(
      List<AuthoringException.Violation> violations, String path, String value) {
    if (value == null || value.isBlank())
      violations.add(new AuthoringException.Violation(path, "REQUIRED", "Value is required"));
    else if (value.indexOf(0) >= 0)
      violations.add(
          new AuthoringException.Violation(
              path, "INVALID", "Value contains an unsupported character"));
  }

  private static EventView view(AuthoringEvent event, boolean edit) {
    boolean writable =
        edit && (event.state() == EventState.DRAFT || event.state() == EventState.PUBLISHED);
    return new EventView(
        event.id(),
        event.organizationRef(),
        event.title(),
        event.description(),
        wire(event.category().name()),
        wire(event.state().name()),
        event.createdAt(),
        event.publishedAt(),
        event.version(),
        event.activities().stream().map(activity -> new ActivityView(activity.id(), activity.title(),
            activity.description(), activity.schedule().start(), activity.schedule().end(),
            activity.location(), activity.onlineLink(), activity.capacity(), activity.registration().open(),
            activity.registration().close(), wire(activity.visibility().name()), activity.cancelled(),
            activity.createdAt(), activity.registration().effectiveOpen(event.publishedAt(), activity.createdAt()),
            activity.registration().effectiveClose(activity.schedule()))).toList(),
        new Capabilities(writable, edit && event.state() == EventState.DRAFT));
  }

  private static String wire(String value) {
    return value.substring(0, 1) + value.substring(1).toLowerCase(Locale.ROOT);
  }
}
