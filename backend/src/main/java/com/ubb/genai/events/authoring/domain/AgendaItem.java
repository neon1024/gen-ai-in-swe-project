package com.ubb.genai.events.authoring.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendaItem(UUID id, OffsetDateTime time, String title, String speaker) {}
