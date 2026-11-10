package com.ubb.genai.events.authoring.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuthoringViews {
  private AuthoringViews() {}

  public record Capabilities(boolean canEdit, boolean canPublish) {}

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
      List<Object> activities,
      Capabilities capabilities) {}
}
