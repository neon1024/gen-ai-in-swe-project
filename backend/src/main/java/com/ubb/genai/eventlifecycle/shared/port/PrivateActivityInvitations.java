package com.ubb.genai.eventlifecycle.shared.port;

import java.util.UUID;

/** Invitations to private activities. */
public interface PrivateActivityInvitations {

    boolean isInvited(UUID activityId, UUID userId);
}
