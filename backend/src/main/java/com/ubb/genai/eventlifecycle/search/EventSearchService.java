package com.ubb.genai.eventlifecycle.search;

import com.ubb.genai.eventlifecycle.search.EventSearchResult.EventSummary;
import com.ubb.genai.eventlifecycle.search.EventSearchResult.OrganizationOption;
import com.ubb.genai.eventlifecycle.shared.access.EventAccessPolicy;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.port.OrganizationDirectory;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Browse, search and filter published events (US-24, US-25). Candidates are loaded in one query and filtered,
 * sorted and paged in memory.
 */
@Service
public class EventSearchService {

    private final LifecycleEventRepository events;
    private final EventAccessPolicy access;
    private final OrganizationDirectory organizations;
    private final Clock clock;

    public EventSearchService(
            LifecycleEventRepository events,
            EventAccessPolicy access,
            OrganizationDirectory organizations,
            Clock clock) {
        this.events = events;
        this.access = access;
        this.organizations = organizations;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EventSearchResult search(EventSearchCriteria criteria, Optional<UUID> userId) {
        Instant now = clock.instant();
        Set<UUID> keywordOrgIds = criteria.keyword() == null
                ? Set.of()
                : organizations.organizationIdsWithNameContaining(criteria.keyword());
        Map<UUID, String> names = new HashMap<>();

        List<Candidate> visible = events.findPublishedWithActivityEndingAfter(now).stream()
                .map(e -> new Candidate(e, EventSearchMatcher.qualifyingActivities(
                        e, now, a -> access.canSeeActivity(a, e.getOrganizationId(), userId))))
                .filter(c -> !c.qualifying().isEmpty())
                .toList();

        List<OrganizationOption> options = visible.stream()
                .map(c -> c.event().getOrganizationId())
                .distinct()
                .map(id -> new OrganizationOption(id, name(names, id)))
                .sorted(Comparator.comparing(OrganizationOption::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(OrganizationOption::id))
                .toList();

        List<Candidate> matching = visible.stream()
                .filter(c -> EventSearchMatcher.matches(c.event(), c.qualifying(), criteria, keywordOrgIds))
                .sorted(Comparator.comparing(Candidate::nextStart, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(c -> c.event().getTitle())
                        .thenComparing(c -> c.event().getId()))
                .toList();

        List<EventSummary> page = matching.stream()
                .skip((long) criteria.page() * criteria.size())
                .limit(criteria.size())
                .map(c -> summary(c, name(names, c.event().getOrganizationId())))
                .toList();
        return new EventSearchResult(page, criteria.page(), criteria.size(), matching.size(), options);
    }

    private String name(Map<UUID, String> cache, UUID organizationId) {
        return cache.computeIfAbsent(organizationId, id -> organizations.organizationName(id).orElse(""));
    }

    private static EventSummary summary(Candidate c, String organizationName) {
        LifecycleEvent e = c.event();
        return new EventSummary(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getCategory(),
                e.getOrganizationId(),
                organizationName,
                c.nextStart());
    }

    private record Candidate(LifecycleEvent event, List<LifecycleActivity> qualifying) {

        Instant nextStart() {
            return qualifying.stream()
                    .map(LifecycleActivity::getStartsAt)
                    .filter(java.util.Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
        }
    }
}
