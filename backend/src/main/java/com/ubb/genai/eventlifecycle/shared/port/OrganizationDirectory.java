package com.ubb.genai.eventlifecycle.shared.port;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Organization membership lookups. */
public interface OrganizationDirectory {

    boolean isMember(UUID organizationId, UUID userId);

    boolean isOrgAdmin(UUID organizationId, UUID userId);

    Optional<String> organizationName(UUID organizationId);

    /** Ids of organizations whose name contains the keyword, ignoring case. */
    Set<UUID> organizationIdsWithNameContaining(String keyword);
}
