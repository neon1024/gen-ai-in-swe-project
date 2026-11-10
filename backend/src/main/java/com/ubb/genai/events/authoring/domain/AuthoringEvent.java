package com.ubb.genai.events.authoring.domain;

import java.time.Instant;
import java.util.UUID;

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
    long version) {
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
    return new AuthoringEvent(
        UUID.randomUUID(),
        org,
        owner,
        title,
        description,
        category,
        EventState.DRAFT,
        now,
        null,
        0);
  }
}
