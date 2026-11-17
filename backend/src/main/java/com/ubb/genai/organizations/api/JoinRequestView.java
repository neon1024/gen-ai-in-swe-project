package com.ubb.genai.organizations.api;

import java.time.Instant;
import java.util.UUID;

public record JoinRequestView(UUID id, UUID applicantId, Instant createdAt) {
}
