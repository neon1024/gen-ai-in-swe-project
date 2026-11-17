package com.ubb.genai.organizations.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTeamMembershipRepository extends JpaRepository<EventTeamMembershipEntity, UUID> {

    Optional<EventTeamMembershipEntity> findByEventIdAndUserId(UUID eventId, UUID userId);

    long countByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    long deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
