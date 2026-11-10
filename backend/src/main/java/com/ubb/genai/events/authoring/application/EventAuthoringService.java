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

  public EventAuthoringService(
      AuthoringIdentityPort identity,
      OrganizationAccessPort organizations,
      EventTeamAccessPort team,
      AuthoringEventRepository repository,
      Clock clock) {
    this.identity = identity;
    this.organizations = organizations;
    this.team = team;
    this.repository = repository;
    this.clock = clock;
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
    var org = organization(actor.subject(), event.organizationRef());
    if (org == OrganizationAccessPort.Role.NONE) throw notFound();
    boolean edit =
        org == OrganizationAccessPort.Role.ORG_ADMIN
            || actor.subject().equals(event.ownerSubject());
    if (!edit) {
      var role = team.roleFor(actor.subject(), id);
      if (role == null) throw AuthoringException.unavailable();
      if (role == EventTeamAccessPort.Role.NONE) throw notFound();
      edit = role == EventTeamAccessPort.Role.EDITOR;
    }
    return view(event, edit);
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
        List.of(),
        new Capabilities(writable, edit && event.state() == EventState.DRAFT));
  }

  private static String wire(String value) {
    return value.substring(0, 1) + value.substring(1).toLowerCase(Locale.ROOT);
  }
}
