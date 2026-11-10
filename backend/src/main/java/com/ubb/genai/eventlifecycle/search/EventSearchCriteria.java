package com.ubb.genai.eventlifecycle.search;

import com.ubb.genai.eventlifecycle.shared.model.EventCategory;
import java.time.Instant;
import java.util.UUID;

/** Validated discovery filters; a null filter means "no restriction". */
public record EventSearchCriteria(
        String keyword, Instant from, Instant to, UUID organizationId, EventCategory category, int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public EventSearchCriteria {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }
}
