package com.ubb.genai.events.authoring.application;

import com.ubb.genai.events.authoring.api.AuthoringViews.ActivityView;
import com.ubb.genai.events.authoring.api.AuthoringViews.AgendaView;
import com.ubb.genai.events.authoring.api.AuthoringViews.Capabilities;
import com.ubb.genai.events.authoring.api.AuthoringViews.EventView;
import com.ubb.genai.events.authoring.domain.ActivitySchedule;
import com.ubb.genai.events.authoring.domain.ActivityVisibility;
import com.ubb.genai.events.authoring.domain.AgendaItem;
import com.ubb.genai.events.authoring.domain.AuthoringActivity;
import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import com.ubb.genai.events.authoring.domain.EventCategory;
import com.ubb.genai.events.authoring.domain.EventState;
import com.ubb.genai.events.authoring.domain.RegistrationWindow;
import com.ubb.genai.events.authoring.persistence.AuthoringEventRepository;
import com.ubb.genai.events.authoring.ports.AuthoringIdentityPort;
import com.ubb.genai.events.authoring.ports.EventTeamAccessPort;
import com.ubb.genai.events.authoring.ports.OrganizationAccessPort;
import com.ubb.genai.events.authoring.ports.RegistrationAuthoringPort;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.transaction.annotation.Isolation;
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

  public record AgendaCommand(OffsetDateTime time, String title, String speaker, Long expectedVersion) {}

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

  public record EventDetailsCommand(Long expectedVersion, String title, String description, EventCategory category) {}

  @Transactional
  public EventView editEventDetails(UUID eventId, EventDetailsCommand command) {
    if (command == null) throw invalid("event", "Event details are required");
    if (command.expectedVersion() == null || command.expectedVersion() < 0)
      throw invalid("expectedVersion", "Nonnegative version is required");
    var actor = actor();
    var event = lockEvent(eventId);
    requireWritable(actor, event, command.expectedVersion());
    var violations = new ArrayList<AuthoringException.Violation>();
    required(violations, "title", command.title());
    required(violations, "description", command.description());
    if (command.category() == null)
      violations.add(new AuthoringException.Violation("category", "REQUIRED", "Category is required"));
    if (!violations.isEmpty())
      throw new AuthoringException(400, "INVALID_INPUT", "Invalid request", violations);
    return view(repository.replace(event.withDetails(command.title(), command.description(), command.category())), true);
  }

  @Transactional
  public EventView publish(UUID eventId, long expectedVersion) {
    var actor = actor();
    var event = lockEvent(eventId);
    requireWritable(actor, event, expectedVersion);
    if (event.state() != EventState.DRAFT)
      throw new AuthoringException(409, "STATE_CONFLICT", "Only Draft events can be published");
    var violations = PublicationReadiness.violations(event);
    if (!violations.isEmpty())
      throw new AuthoringException(422, "PUBLICATION_NOT_READY", "Event is not ready for publication", violations);
    return view(repository.replace(event.publish(clock.instant())), true);
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

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
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

  private AuthoringEvent lockEvent(UUID eventId) {
    return repository.findForUpdate(eventId).orElseThrow(EventAuthoringService::notFound);
  }

  private void requireWritable(
      AuthoringIdentityPort.Actor actor, AuthoringEvent event, long expectedVersion) {
    if (!canEdit(actor, event)) throw new AuthoringException(403, "FORBIDDEN", "Action denied");
    if (event.state() != EventState.DRAFT && event.state() != EventState.PUBLISHED)
      throw new AuthoringException(409, "STATE_CONFLICT", "Event is read only");
    if (expectedVersion < 0) throw invalid("expectedVersion", "Version must be nonnegative");
    if (event.version() != expectedVersion)
      throw new AuthoringException(409, "VERSION_CONFLICT", "Version conflict");
  }

  @Transactional
  public EventView addActivity(UUID eventId, ActivityCommand command) {
    checkCommand(command);
    var actor = actor();
    var event = lockEvent(eventId);
    requireWritable(actor, event, command.expectedVersion());
    var activities = new ArrayList<>(event.activities());
    activities.add(activity(command, UUID.randomUUID(), clock.instant().truncatedTo(ChronoUnit.MICROS), false));
    return view(repository.replace(event.withActivities(activities)), true);
  }

  @Transactional
  public EventView updateActivity(UUID eventId, UUID activityId, ActivityCommand command) {
    checkCommand(command);
    var actor = actor();
    var event = lockEvent(eventId);
    var previous = activity(event, activityId);
    requireWritable(actor, event, command.expectedVersion());
    var replacement = activity(command, activityId, previous.createdAt(), previous.cancelled()).withAgenda(previous.agenda());
    if (previous.agenda().stream().anyMatch(item -> !replacement.schedule().contains(item.time())))
      throw invalid("schedule", "Activity schedule would exclude an existing agenda item");
    var activities = event.activities().stream().map(a -> a.id().equals(activityId) ? replacement : a).toList();
    // Signups exist only after publication; Draft capacity edits never consult registrations.
    if (event.state() != EventState.PUBLISHED || replacement.capacity() == previous.capacity())
      return view(repository.replace(event.withActivities(activities)), true);
    return registration.withLockedActivity(new RegistrationAuthoringPort.GuardRequest(eventId,
        activityId, RegistrationAuthoringPort.MutationKind.CAPACITY_CHANGE), context -> {
      if (context == null) throw AuthoringException.unavailable();
      long confirmed = context.confirmedCount();
      if (replacement.capacity() < confirmed)
        throw new AuthoringException(409, "SIGNUP_CONFLICT", "Capacity is below confirmed signup count");
      var updated = repository.replace(event.withActivities(activities));
      if (replacement.capacity() > previous.capacity()) {
        var now = clock.instant();
        var startsAt = replacement.schedule().start().toInstant();
        if (now.isBefore(startsAt))
          context.promoteEligibleWaitlist(replacement.capacity(), startsAt, now);
      }
      return view(updated, true);
    });
  }

  @Transactional
  public EventView removeActivity(UUID eventId, UUID activityId, long expectedVersion) {
    var actor = actor();
    var event = lockEvent(eventId);
    activity(event, activityId);
    requireWritable(actor, event, expectedVersion);
    if (event.state() == EventState.PUBLISHED && event.activities().size() == 1)
      throw new AuthoringException(409, "STATE_CONFLICT", "Published events must retain an activity");
    return registration.withLockedActivity(new RegistrationAuthoringPort.GuardRequest(eventId,
        activityId, RegistrationAuthoringPort.MutationKind.REMOVE), context -> {
      if (context == null) throw AuthoringException.unavailable();
      if (context.hasAnySignup()) throw new AuthoringException(409, "SIGNUP_CONFLICT", "Activity has signup history");
      var remaining = event.activities().stream().filter(a -> !a.id().equals(activityId)).toList();
      return view(repository.replace(event.withActivities(remaining)), true);
    });
  }

  @Transactional
  public EventView addAgenda(UUID eventId, UUID activityId, AgendaCommand command) {
    checkAgendaCommand(command);
    var actor = actor();
    var event = lockEvent(eventId);
    var activity = activity(event, activityId);
    requireWritable(actor, event, command.expectedVersion());
    var items = new ArrayList<>(activity.agenda());
    items.add(agenda(command, UUID.randomUUID(), activity.schedule()));
    return replaceActivity(event, activity.withAgenda(items));
  }

  @Transactional
  public EventView updateAgenda(UUID eventId, UUID activityId, UUID itemId, AgendaCommand command) {
    checkAgendaCommand(command);
    var actor = actor();
    var event = lockEvent(eventId);
    var activity = activity(event, activityId);
    agendaItem(activity, itemId);
    requireWritable(actor, event, command.expectedVersion());
    var replacement = agenda(command, itemId, activity.schedule());
    var items = activity.agenda().stream().map(i -> i.id().equals(itemId) ? replacement : i).toList();
    return replaceActivity(event, activity.withAgenda(items));
  }

  @Transactional
  public EventView removeAgenda(UUID eventId, UUID activityId, UUID itemId, long expectedVersion) {
    var actor = actor();
    var event = lockEvent(eventId);
    var activity = activity(event, activityId);
    agendaItem(activity, itemId);
    requireWritable(actor, event, expectedVersion);
    return replaceActivity(event, activity.withAgenda(activity.agenda().stream()
        .filter(i -> !i.id().equals(itemId)).toList()));
  }

  private EventView replaceActivity(AuthoringEvent event, AuthoringActivity replacement) {
    var activities = event.activities().stream().map(a -> a.id().equals(replacement.id()) ? replacement : a).toList();
    return view(repository.replace(event.withActivities(activities)), true);
  }

  private static AgendaItem agendaItem(AuthoringActivity activity, UUID itemId) {
    return activity.agenda().stream().filter(i -> i.id().equals(itemId)).findFirst().orElseThrow(EventAuthoringService::notFound);
  }

  private static void checkAgendaCommand(AgendaCommand command) {
    if (command == null) throw invalid("agenda", "Agenda item is required");
    if (command.expectedVersion() == null || command.expectedVersion() < 0)
      throw invalid("expectedVersion", "Nonnegative version is required");
  }

  private static AgendaItem agenda(AgendaCommand command, UUID id, ActivitySchedule schedule) {
    var violations = new ArrayList<AuthoringException.Violation>();
    required(violations, "title", command.title());
    optionalText(violations, "speaker", command.speaker());
    if (!violations.isEmpty()) throw new AuthoringException(400, "INVALID_INPUT", "Invalid request", violations);
    if (!schedule.contains(command.time())) throw invalid("time", "Agenda time must be within activity interval");
    return new AgendaItem(id, microseconds(command.time()), command.title(), command.speaker());
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
      var submittedSchedule = new ActivitySchedule(command.start(), command.end());
      var submittedWindow = new RegistrationWindow(command.registrationOpen(), command.registrationClose());
      submittedWindow.validate(submittedSchedule);
      var schedule = new ActivitySchedule(microseconds(command.start()), microseconds(command.end()));
      var window = new RegistrationWindow(microseconds(command.registrationOpen()), microseconds(command.registrationClose()));
      window.validate(schedule);
      return new AuthoringActivity(id, command.title(), command.description(), schedule,
          command.location(), command.onlineLink(), command.capacity(), window,
          ActivityVisibility.PUBLIC, cancelled, createdAt);
    } catch (IllegalArgumentException exception) {
      throw invalid("schedule", exception.getMessage());
    }
  }

  private static OffsetDateTime microseconds(OffsetDateTime value) {
    return value == null ? null : value.truncatedTo(ChronoUnit.MICROS);
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
            activity.registration().effectiveClose(activity.schedule()),
            activity.agenda().stream().map(item -> new AgendaView(item.id(), item.time(), item.title(), item.speaker())).toList())).toList(),
        new Capabilities(writable, edit && event.state() == EventState.DRAFT));
  }

  private static String wire(String value) {
    return value.substring(0, 1) + value.substring(1).toLowerCase(Locale.ROOT);
  }
}
