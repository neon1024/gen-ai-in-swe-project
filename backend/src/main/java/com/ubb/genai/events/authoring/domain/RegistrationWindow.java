package com.ubb.genai.events.authoring.domain;

import java.time.Instant;
import java.time.OffsetDateTime;

public record RegistrationWindow(OffsetDateTime open, OffsetDateTime close) {
  public void validate(ActivitySchedule schedule) {
    Instant closing = effectiveClose(schedule);
    if (closing.isAfter(schedule.start().toInstant()))
      throw new IllegalArgumentException("Registration must close by activity start");
    if (open != null && !open.toInstant().isBefore(closing))
      throw new IllegalArgumentException("Registration opening must precede closing");
  }

  public Instant effectiveOpen(Instant publication, Instant createdAt) {
    if (publication == null) return null;
    Instant result = publication.isAfter(createdAt) ? publication : createdAt;
    return open != null && open.toInstant().isAfter(result) ? open.toInstant() : result;
  }

  public Instant effectiveClose(ActivitySchedule schedule) {
    return close == null ? schedule.start().toInstant() : close.toInstant();
  }
}
