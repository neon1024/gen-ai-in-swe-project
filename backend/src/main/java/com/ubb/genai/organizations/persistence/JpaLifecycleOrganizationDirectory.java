package com.ubb.genai.organizations.persistence;

import com.ubb.genai.eventlifecycle.shared.port.OrganizationDirectory;
import com.ubb.genai.events.authoring.ports.OrganizationAccessPort;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JpaLifecycleOrganizationDirectory implements OrganizationDirectory {

    private final OrganizationAccessPort organizationAccess;
    private final OrganizationRepository organizations;

    public JpaLifecycleOrganizationDirectory(
            OrganizationAccessPort organizationAccess,
            OrganizationRepository organizations) {
        this.organizationAccess = organizationAccess;
        this.organizations = organizations;
    }

    @Override
    public boolean isMember(UUID organizationId, UUID userId) {
        return roleFor(organizationId, userId) != OrganizationAccessPort.Role.NONE;
    }

    @Override
    public boolean isOrgAdmin(UUID organizationId, UUID userId) {
        return roleFor(organizationId, userId) == OrganizationAccessPort.Role.ORG_ADMIN;
    }

    @Override
    public Optional<String> organizationName(UUID organizationId) {
        return organizations.findById(organizationId)
                .filter(OrganizationEntity::active)
                .map(OrganizationEntity::name);
    }

    @Override
    public Set<UUID> organizationIdsWithNameContaining(String keyword) {
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return organizations.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(organization -> organization.name().toLowerCase(Locale.ROOT).contains(normalized))
                .map(OrganizationEntity::id)
                .collect(Collectors.toUnmodifiableSet());
    }

    private OrganizationAccessPort.Role roleFor(UUID organizationId, UUID userId) {
        return organizationAccess.forActor(userId.toString(), organizationId.toString());
    }
}
