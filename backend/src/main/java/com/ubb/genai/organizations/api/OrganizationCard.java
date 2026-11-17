package com.ubb.genai.organizations.api;

import com.ubb.genai.organizations.domain.JoinRequestStatus;
import com.ubb.genai.organizations.domain.OrganizationRole;
import java.util.UUID;

public record OrganizationCard(
        UUID id,
        String name,
        String description,
        boolean member,
        OrganizationRole role,
        JoinRequestStatus requestStatus) {
}
