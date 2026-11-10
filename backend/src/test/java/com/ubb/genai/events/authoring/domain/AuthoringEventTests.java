package com.ubb.genai.events.authoring.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuthoringEventTests {
  @Test
  void US14_draftStartsWithActualOwnerAndServerState() {
    var now = Instant.parse("2026-11-10T10:00:00Z");
    var event =
        AuthoringEvent.draft("org", "actual", "Title", "Description", EventCategory.WORKSHOP, now);
    assertNotNull(event);
    assertNotNull(event.id());
    assertEquals("actual", event.ownerSubject());
    assertEquals(EventState.DRAFT, event.state());
    assertEquals(now, event.createdAt());
    assertNull(event.publishedAt());
    assertEquals(0, event.version());
  }

  @Test
  void draftCreationUsesDatabaseTimestampPrecision() {
    var event =
        AuthoringEvent.draft(
            "org",
            "owner",
            "T",
            "D",
            EventCategory.OTHER,
            Instant.parse("2026-11-10T10:00:00.123456789Z"));
    assertEquals(Instant.parse("2026-11-10T10:00:00.123456Z"), event.createdAt());
  }

  @Test
  void draftRefusesEmbeddedNulInMetadata() {
    String invalid = "before" + (char) 0 + "after";
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AuthoringEvent.draft("org", "owner", invalid, "D", EventCategory.OTHER, Instant.now()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AuthoringEvent.draft("org", "owner", "T", invalid, EventCategory.OTHER, Instant.now()));
    assertThrows(
        IllegalArgumentException.class,
        () -> AuthoringEvent.draft(invalid, "owner", "T", "D", EventCategory.OTHER, Instant.now()));
  }

  @Test
  void US14_requiredMetadata() {
    for (String value : new String[] {null, "", " ", "\t\n"}) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              AuthoringEvent.draft("org", "actor", value, "D", EventCategory.OTHER, Instant.now()));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              AuthoringEvent.draft("org", "actor", "T", value, EventCategory.OTHER, Instant.now()));
      assertThrows(
          IllegalArgumentException.class,
          () -> AuthoringEvent.draft(value, "actor", "T", "D", EventCategory.OTHER, Instant.now()));
      assertThrows(
          IllegalArgumentException.class,
          () -> AuthoringEvent.draft("org", value, "T", "D", EventCategory.OTHER, Instant.now()));
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> AuthoringEvent.draft("org", "actor", "T", "D", null, Instant.now()));
  }
}
