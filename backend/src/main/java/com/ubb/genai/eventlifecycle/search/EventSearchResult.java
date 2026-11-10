package com.ubb.genai.eventlifecycle.search;

import com.ubb.genai.eventlifecycle.shared.model.EventCategory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventSearchResult(
        List<EventSummary> items, int page, int size, long totalItems, List<OrganizationOption> organizations) {

    public record EventSummary(
            UUID id,
            String title,
            String description,
            EventCategory category,
            UUID organizationId,
            String organizationName,
            Instant nextActivityStartsAt) {
    }

    public record OrganizationOption(UUID id, String name) {
    }
}
