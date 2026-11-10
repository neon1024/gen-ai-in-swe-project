package com.ubb.genai.events.authoring.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;

public record AuthoringEvent(
    UUID id,
    String organizationRef,
    String ownerSubject,
    String title,
    String description,
    EventCategory category,
    EventState state,
    Instant createdAt,
    Instant publishedAt,
    long version,
    List<AuthoringActivity> activities) {
  public AuthoringEvent {
    activities = List.copyOf(activities);
  }

  public AuthoringEvent(UUID id, String organizationRef, String ownerSubject, String title,
      String description, EventCategory category, EventState state, Instant createdAt,
      Instant publishedAt, long version) {
    this(id, organizationRef, ownerSubject, title, description, category, state, createdAt,
        publishedAt, version, List.of());
  }

  public AuthoringEvent withActivities(List<AuthoringActivity> replacements) {
    return new AuthoringEvent(id, organizationRef, ownerSubject, title, description, category,
        state, createdAt, publishedAt, version, replacements);
  }

  public static AuthoringEvent draft(
      String org,
      String owner,
      String title,
      String description,
      EventCategory category,
      Instant now) {
    if (org == null
        || org.isBlank()
        || owner == null
        || owner.isBlank()
        || title == null
        || title.isBlank()
        || description == null
        || description.isBlank()
        || category == null
        || now == null) throw new IllegalArgumentException("Required draft metadata missing");
    if (org.indexOf(0) >= 0 || title.indexOf(0) >= 0 || description.indexOf(0) >= 0) {
      throw new IllegalArgumentException("Draft metadata contains an unsupported character");
    }
    return new AuthoringEvent(
        UUID.randomUUID(),
        org,
        owner,
        title,
        description,
        category,
        EventState.DRAFT,
        now.truncatedTo(ChronoUnit.MICROS),
        null,
        0);
  }
}
