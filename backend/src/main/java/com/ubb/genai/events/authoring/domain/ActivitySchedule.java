package com.ubb.genai.events.authoring.domain;

import java.time.OffsetDateTime;

public record ActivitySchedule(OffsetDateTime start, OffsetDateTime end) {
  public ActivitySchedule {
    if (start == null || end == null || !end.toInstant().isAfter(start.toInstant()))
      throw new IllegalArgumentException("Activity end must be after start");
  }

  public boolean contains(OffsetDateTime time) {
    return time != null && !time.toInstant().isBefore(start.toInstant())
        && !time.toInstant().isAfter(end.toInstant());
  }
}
