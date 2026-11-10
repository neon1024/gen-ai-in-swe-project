package com.ubb.genai.eventlifecycle.search;

import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Pure filtering rules of the discovery search (US-24, US-25, BR-18). */
final class EventSearchMatcher {

    private EventSearchMatcher() {
    }

    /** Scheduled activities that have not ended yet and that the viewer may see. */
    static List<LifecycleActivity> qualifyingActivities(
            LifecycleEvent event, Instant now, Predicate<LifecycleActivity> visible) {
        return event.getActivities().stream()
                .filter(a -> !a.isCancelled())
                .filter(a -> a.getEndsAt() != null && a.getEndsAt().isAfter(now))
                .filter(visible)
                .toList();
    }

    static boolean matches(
            LifecycleEvent event,
            List<LifecycleActivity> qualifying,
            EventSearchCriteria criteria,
            Set<UUID> orgIdsMatchingKeyword) {
        if (qualifying.isEmpty()) {
            return false;
        }
        if (criteria.category() != null && criteria.category() != event.getCategory()) {
            return false;
        }
        if (criteria.organizationId() != null && !criteria.organizationId().equals(event.getOrganizationId())) {
            return false;
        }
        if (criteria.keyword() != null && !matchesKeyword(event, criteria.keyword(), orgIdsMatchingKeyword)) {
            return false;
        }
        return qualifying.stream().anyMatch(a -> overlaps(a, criteria.from(), criteria.to()));
    }

    private static boolean matchesKeyword(LifecycleEvent event, String keyword, Set<UUID> orgIds) {
        String needle = keyword.toLowerCase(Locale.ROOT);
        return contains(event.getTitle(), needle)
                || contains(event.getDescription(), needle)
                || orgIds.contains(event.getOrganizationId());
    }

    private static boolean contains(String text, String lowerNeedle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerNeedle);
    }

    private static boolean overlaps(LifecycleActivity activity, Instant from, Instant to) {
        boolean startsInTime = to == null || activity.getStartsAt() == null || !activity.getStartsAt().isAfter(to);
        boolean endsInTime = from == null || !activity.getEndsAt().isBefore(from);
        return startsInTime && endsInTime;
    }
}
