package com.ubb.genai.organizations.persistence;

import com.ubb.genai.eventlifecycle.shared.port.EventRole;
import com.ubb.genai.events.authoring.ports.EventTeamAccessPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaEventTeamDirectory implements EventTeamAccessPort {

    private final EventTeamMembershipRepository memberships;

    public JpaEventTeamDirectory(EventTeamMembershipRepository memberships) {
        this.memberships = memberships;
    }

    @Override
    public Role roleFor(String subject, UUID eventId) {
        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Role.NONE;
        }
        return memberships.findByEventIdAndUserId(eventId, userId)
                .map(EventTeamMembershipEntity::role)
                .map(role -> role == EventRole.EDITOR ? Role.EDITOR : Role.STAFF)
                .orElse(Role.NONE);
    }
}
