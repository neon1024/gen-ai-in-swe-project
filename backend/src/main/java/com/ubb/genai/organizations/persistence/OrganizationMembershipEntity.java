package com.ubb.genai.organizations.persistence;

import com.ubb.genai.organizations.domain.OrganizationRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_membership", schema = "organizations")
public class OrganizationMembershipEntity {

    @Id
    UUID id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    OrganizationRole role;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected OrganizationMembershipEntity() {
    }

    public OrganizationMembershipEntity(UUID organizationId, UUID userId, OrganizationRole role, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID userId() {
        return userId;
    }

    public OrganizationRole role() {
        return role;
    }

    public void changeRole(OrganizationRole role) {
        this.role = role;
    }
}
