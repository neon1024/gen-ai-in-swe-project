package com.ubb.genai.eventlifecycle.shared.port.stub;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.ubb.genai.eventlifecycle.shared.port.OrganizationDirectory;
import org.springframework.stereotype.Component;

/**
 * Stub: replace with the real implementation from All16 at reconciliation.
 */
@Component
public class StubOrganizationDirectory implements OrganizationDirectory {

    @Override
    public boolean isMember(UUID organizationId, UUID userId) {
        return false;
    }

    @Override
    public boolean isOrgAdmin(UUID organizationId, UUID userId) {
        return false;
    }

    @Override
    public Optional<String> organizationName(UUID organizationId) {
        return Optional.empty();
    }

    @Override
    public Set<UUID> organizationIdsWithNameContaining(String keyword) {
        return Set.of();
    }
}
