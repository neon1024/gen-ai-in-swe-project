package com.ubb.genai.events.authoring.ports;

import java.util.UUID;

/**
 * Resolves current external Editor/Staff assignments. The aggregate alone supplies current Owner.
 */
public interface EventTeamAccessPort {
  enum Role {
    NONE,
    EDITOR,
    STAFF
  }

  Role roleFor(String subject, UUID eventId);
}
