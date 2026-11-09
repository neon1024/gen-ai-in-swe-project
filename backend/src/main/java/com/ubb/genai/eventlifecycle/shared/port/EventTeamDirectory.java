package com.ubb.genai.eventlifecycle.shared.port;

import java.util.Optional;
import java.util.UUID;

/** Event-team role lookups. */
public interface EventTeamDirectory {

    Optional<EventRole> roleOf(UUID eventId, UUID userId);
}
