package com.ubb.genai.events.authoring.domain;

import java.time.Instant;
import java.util.UUID;

public record AuthoringActivity(
    UUID id, String title, String description, ActivitySchedule schedule,
    String location, String onlineLink, int capacity, RegistrationWindow registration,
    ActivityVisibility visibility, boolean cancelled, Instant createdAt) {}
