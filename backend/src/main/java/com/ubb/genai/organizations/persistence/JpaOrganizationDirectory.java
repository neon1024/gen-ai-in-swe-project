package com.ubb.genai.organizations.persistence;

import com.ubb.genai.events.authoring.ports.OrganizationAccessPort;
import com.ubb.genai.organizations.domain.OrganizationRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class JpaOrganizationDirectory implements OrganizationAccessPort {

    private final OrganizationRepository organizations;
    private final OrganizationMembershipRepository memberships;

    public JpaOrganizationDirectory(OrganizationRepository organizations, OrganizationMembershipRepository memberships) {
        this.organizations = organizations;
        this.memberships = memberships;
    }

    @Override
    public Role forActor(String subject, String organizationRef) {
        UUID userId;
        UUID organizationId;
        try {
            userId = UUID.fromString(subject);
            organizationId = UUID.fromString(organizationRef);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Role.NONE;
        }

        if (findOrganizationForActor(organizationId).isEmpty()) {
            return Role.NONE;
        }
        return memberships.findByOrganizationIdAndUserId(organizationId, userId)
                .map(OrganizationMembershipEntity::role)
                .map(role -> switch (role) {
                    case MEMBER -> Role.MEMBER;
                    case ORGANIZER -> Role.ORGANIZER;
                    case ORG_ADMIN -> Role.ORG_ADMIN;
                })
                .orElse(Role.NONE);
    }

    private Optional<OrganizationEntity> findOrganizationForActor(UUID organizationId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return organizations.findActiveForUpdate(organizationId);
        }
        return organizations.findByIdAndActiveTrue(organizationId);
    }
}
