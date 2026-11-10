package com.ubb.genai.eventlifecycle.shared.port.stub;

import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ubb.genai.eventlifecycle.shared.port.NotificationPort;
import org.springframework.stereotype.Component;

/**
 * Stub: replace with the real implementation from neon1024 at reconciliation.
 */
@Component
public class StubNotificationPort implements NotificationPort {

    private static final Logger LOG = LoggerFactory.getLogger(StubNotificationPort.class);

    @Override
    public void activityCancelled(UUID activityId, Set<UUID> recipients) {
        LOG.info("Activity {} cancelled; notifying users {}", activityId, recipients);
    }

    @Override
    public void eventCancelled(UUID eventId, Set<UUID> recipients) {
        LOG.info("Event {} cancelled; notifying users {}", eventId, recipients);
    }
}
