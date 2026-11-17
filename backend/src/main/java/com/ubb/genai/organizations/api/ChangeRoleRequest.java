package com.ubb.genai.organizations.api;

import com.ubb.genai.organizations.domain.OrganizationRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull OrganizationRole role) {
}
