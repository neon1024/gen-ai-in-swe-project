package com.ubb.genai.eventlifecycle.shared.port.stub;

import com.ubb.genai.eventlifecycle.shared.port.SignupFigures;
import java.util.Set;
import java.util.UUID;

import com.ubb.genai.eventlifecycle.shared.port.SignupPort;
import org.springframework.stereotype.Component;

/**
 * Stub: replace with the real implementation from lucianvaleanu at reconciliation.
 */
@Component
public class StubSignupPort implements SignupPort {

    @Override
    public Set<UUID> cancelAllSignups(UUID activityId) {
        return Set.of();
    }

    @Override
    public SignupFigures figures(UUID activityId) {
        return new SignupFigures(0, 0, 0, 0);
    }

    @Override
    public boolean wasMarkedPresent(UUID activityId, UUID userId) {
        return false;
    }
}
