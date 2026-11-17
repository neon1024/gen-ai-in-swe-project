package com.ubb.genai.auth.domain;

import java.util.UUID;

public record LoginResponse(UUID userId, String email, String username, String displayName, UserStatus status) {}
