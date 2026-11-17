package com.ubb.genai.organizations.persistence;

import com.ubb.genai.organizations.domain.OrganizationRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembershipEntity, UUID> {

    Optional<OrganizationMembershipEntity> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    List<OrganizationMembershipEntity> findAllByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);

    long countByOrganizationIdAndRole(UUID organizationId, OrganizationRole role);

    void deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
