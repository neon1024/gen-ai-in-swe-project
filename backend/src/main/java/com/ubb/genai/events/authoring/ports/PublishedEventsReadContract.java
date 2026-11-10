package com.ubb.genai.events.authoring.ports;

import com.ubb.genai.events.authoring.domain.ActivityVisibility;
import com.ubb.genai.events.authoring.domain.EventCategory;
import com.ubb.genai.events.authoring.domain.EventState;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Internal committed snapshot; consumers supply organization activation and audience policy. */
public interface PublishedEventsReadContract {
  record PublishedActivity(
      UUID id,
      String title,
      String description,
      ActivityVisibility visibility,
      boolean cancelled,
      OffsetDateTime start,
      OffsetDateTime end,
      String location,
      String onlineLink,
      int capacity,
      Instant effectiveRegistrationOpen,
      Instant effectiveRegistrationClose) {}

  record PublishedEvent(
      UUID id,
      String organizationRef,
      String title,
      String description,
      EventCategory category,
      EventState state,
      Instant publishedAt,
      long version,
      List<PublishedActivity> activities) {
    public PublishedEvent {
      activities = List.copyOf(activities);
    }
  }

  List<PublishedEvent> publishedEvents();
}
