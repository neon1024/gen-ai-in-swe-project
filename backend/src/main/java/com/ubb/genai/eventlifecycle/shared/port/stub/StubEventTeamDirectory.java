package com.ubb.genai.eventlifecycle.shared.port.stub;

import com.ubb.genai.eventlifecycle.shared.port.EventRole;
import java.util.Optional;
import java.util.UUID;

import com.ubb.genai.eventlifecycle.shared.port.EventTeamDirectory;
import org.springframework.stereotype.Component;

/**
 * Stub: replace with the real implementation from All16 at reconciliation.
 */
@Component
public class StubEventTeamDirectory implements EventTeamDirectory {

    @Override
    public Optional<EventRole> roleOf(UUID eventId, UUID userId) {
        return Optional.empty();
    }
}
