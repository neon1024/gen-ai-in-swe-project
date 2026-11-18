package com.ubb.genai.organizations.api;

import com.ubb.genai.organizations.domain.OrganizationRole;
import java.util.UUID;

public record MemberView(
        UUID userId,
        String displayName,
        OrganizationRole role,
        long eventTeamAssignmentCount,
        boolean ownsActiveEvents) {
}
