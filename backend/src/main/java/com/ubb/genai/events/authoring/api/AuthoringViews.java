package com.ubb.genai.events.authoring.api;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AuthoringViews {
  private AuthoringViews() {}

  public record Capabilities(boolean canEdit, boolean canPublish) {}

  public record ActivityView(UUID id, String title, String description, OffsetDateTime start,
      OffsetDateTime end, String location, String onlineLink, int capacity,
      OffsetDateTime registrationOpen, OffsetDateTime registrationClose, String visibility,
      boolean cancelled, Instant createdAt, Instant effectiveRegistrationOpen,
      Instant effectiveRegistrationClose) {}

  public record EventView(
      UUID id,
      String organizationRef,
      String title,
      String description,
      String category,
      String state,
      Instant createdAt,
      Instant publishedAt,
      long version,
      List<ActivityView> activities,
      Capabilities capabilities) {}
}
