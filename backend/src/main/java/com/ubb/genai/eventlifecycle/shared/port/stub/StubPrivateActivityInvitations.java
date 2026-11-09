package com.ubb.genai.eventlifecycle.shared.port.stub;

import java.util.UUID;

import com.ubb.genai.eventlifecycle.shared.port.PrivateActivityInvitations;
import org.springframework.stereotype.Component;

/**
 * Stub: replace with the real implementation from jose03pedro at reconciliation.
 */
@Component
public class StubPrivateActivityInvitations implements PrivateActivityInvitations {

    @Override
    public boolean isInvited(UUID activityId, UUID userId) {
        return false;
    }
}
