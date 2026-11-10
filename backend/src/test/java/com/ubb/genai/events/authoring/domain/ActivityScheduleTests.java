package com.ubb.genai.events.authoring.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class ActivityScheduleTests {
  final OffsetDateTime start = OffsetDateTime.parse("2026-12-01T10:00:00+02:00");
  final OffsetDateTime end = start.plusHours(2);

  @Test void BR07_endMustBeStrictlyLaterByInstant() {
    assertThrows(IllegalArgumentException.class, () -> new ActivitySchedule(start, start));
    assertThrows(IllegalArgumentException.class, () -> new ActivitySchedule(start, start.minusNanos(1)));
    assertThrows(IllegalArgumentException.class, () -> new ActivitySchedule(null, end));
    assertThrows(IllegalArgumentException.class, () -> new ActivitySchedule(start, null));
  }
  @Test void BR07_endpointsAreInclusiveAndOffsetsRetained() {
    var schedule = new ActivitySchedule(start, end);
    assertTrue(schedule.contains(start));
    assertTrue(schedule.contains(end));
    assertFalse(schedule.contains(start.minusNanos(1)));
    assertFalse(schedule.contains(end.plusNanos(1)));
    assertEquals(start, schedule.start());
  }
  @Test void BR08_defaultsFollowPublicationCreationAndStart() {
    var window = new RegistrationWindow(null, null);
    var schedule = new ActivitySchedule(start, end);
    assertEquals(start.toInstant(), window.effectiveClose(schedule));
    var publication = start.minusDays(4).toInstant();
    var created = start.minusDays(2).toInstant();
    assertEquals(created, window.effectiveOpen(publication, created));
    assertEquals(publication, window.effectiveOpen(publication, publication.minusSeconds(1)));
    assertNull(window.effectiveOpen(null, created));
  }
  @Test void BR08_explicitWindowMustBeOrderedAndCloseByStart() {
    var schedule = new ActivitySchedule(start, end);
    assertThrows(IllegalArgumentException.class, () -> new RegistrationWindow(null, start.plusSeconds(1)).validate(schedule));
    assertThrows(IllegalArgumentException.class, () -> new RegistrationWindow(start, start).validate(schedule));
    assertThrows(IllegalArgumentException.class, () -> new RegistrationWindow(start.plusSeconds(1), null).validate(schedule));
    var valid = new RegistrationWindow(start.minusDays(3), start.minusHours(1));
    assertDoesNotThrow(() -> valid.validate(schedule));
    assertEquals(start.minusDays(2).toInstant(), valid.effectiveOpen(start.minusDays(2).toInstant(), start.minusDays(4).toInstant()));
    assertEquals(start.minusHours(1).toInstant(), valid.effectiveClose(schedule));
  }
}
