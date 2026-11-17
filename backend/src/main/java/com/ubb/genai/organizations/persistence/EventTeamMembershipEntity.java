package com.ubb.genai.organizations.persistence;

import com.ubb.genai.eventlifecycle.shared.port.EventRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "event_team_membership", schema = "organizations")
public class EventTeamMembershipEntity {

    @Id
    UUID id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(name = "event_id", nullable = false)
    UUID eventId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    EventRole role;

    protected EventTeamMembershipEntity() {
    }

    public UUID userId() {
        return userId;
    }

    public UUID eventId() {
        return eventId;
    }

    public EventRole role() {
        return role;
    }
}
