package com.ubb.genai.organizations.persistence;

import com.ubb.genai.organizations.domain.JoinRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "join_request", schema = "organizations")
public class JoinRequestEntity {

    @Id
    UUID id;

    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    @Column(name = "applicant_id", nullable = false)
    UUID applicantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    JoinRequestStatus status;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected JoinRequestEntity() {
    }

    public JoinRequestEntity(UUID organizationId, UUID applicantId, Instant now) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.applicantId = applicantId;
        this.status = JoinRequestStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID applicantId() {
        return applicantId;
    }

    public JoinRequestStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void decide(JoinRequestStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }
}
