package com.ubb.genai.eventlifecycle.shared.port;

import java.util.Set;
import java.util.UUID;

/** Sends cancellation notifications. */
public interface NotificationPort {

    void activityCancelled(UUID activityId, Set<UUID> recipients);

    void eventCancelled(UUID eventId, Set<UUID> recipients);
}
