package com.ubb.genai.events.authoring.domain;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Comparator;

public record AuthoringActivity(
    UUID id, String title, String description, ActivitySchedule schedule,
    String location, String onlineLink, int capacity, RegistrationWindow registration,
    ActivityVisibility visibility, boolean cancelled, Instant createdAt, List<AgendaItem> agenda) {
  public AuthoringActivity {
    agenda = agenda.stream().sorted(Comparator.comparing((AgendaItem item) -> item.time().toInstant())
        .thenComparing(AgendaItem::id)).toList();
  }

  public AuthoringActivity(UUID id, String title, String description, ActivitySchedule schedule,
      String location, String onlineLink, int capacity, RegistrationWindow registration,
      ActivityVisibility visibility, boolean cancelled, Instant createdAt) {
    this(id, title, description, schedule, location, onlineLink, capacity, registration, visibility,
        cancelled, createdAt, List.of());
  }

  public AuthoringActivity withAgenda(List<AgendaItem> items) {
    return new AuthoringActivity(id, title, description, schedule, location, onlineLink, capacity,
        registration, visibility, cancelled, createdAt, items);
  }
}
