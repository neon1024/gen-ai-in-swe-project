package com.ubb.genai.eventlifecycle.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.EventCategory;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventSearchMatcherTest {

    private static final Instant NOW = Instant.parse("2031-01-01T09:00:00Z");
    private static final Instant TEN = Instant.parse("2031-01-01T10:00:00Z");
    private static final Instant TWELVE = Instant.parse("2031-01-01T12:00:00Z");

    private final UUID orgId = UUID.randomUUID();

    private LifecycleEvent event(String title, String description, EventCategory category, UUID org) {
        return LifecycleEvent.draft(org, title, description, category);
    }

    private LifecycleActivity activity(Instant start, Instant end, ActivityVisibility visibility) {
        return LifecycleActivity.scheduled("Activity", start, end, visibility);
    }

    private EventSearchCriteria criteria(String keyword, Instant from, Instant to, UUID org, EventCategory cat) {
        return new EventSearchCriteria(keyword, from, to, org, cat, 0, 20);
    }

    @Test
    @DisplayName("US-24 only upcoming scheduled visible activities qualify")
    void us24_onlyUpcomingScheduledVisibleActivitiesQualify() {
        LifecycleEvent e = event("Title", "Desc", EventCategory.WORKSHOP, orgId);
        LifecycleActivity upcoming = activity(TEN, TWELVE, ActivityVisibility.PUBLIC);
        LifecycleActivity ended = activity(NOW.minusSeconds(7200), NOW, ActivityVisibility.PUBLIC);
        LifecycleActivity cancelled = activity(TEN, TWELVE, ActivityVisibility.PUBLIC);
        cancelled.cancel(NOW);
        LifecycleActivity hidden = activity(TEN, TWELVE, ActivityVisibility.PRIVATE);
        for (LifecycleActivity a : List.of(upcoming, ended, cancelled, hidden)) {
            e.addActivity(a);
        }

        List<LifecycleActivity> qualifying = EventSearchMatcher.qualifyingActivities(
                e, NOW, a -> a.getVisibility() == ActivityVisibility.PUBLIC);

        assertThat(qualifying).containsExactly(upcoming);
    }

    @Test
    @DisplayName("US-24 the keyword is trimmed, case-insensitive and literal")
    void us24_keywordIsTrimmedCaseInsensitiveAndLiteral() {
        UUID keywordOrg = UUID.randomUUID();
        LifecycleEvent e = event("Intro to 100% Java_Basics", "Hands-on lab", EventCategory.WORKSHOP, orgId);
        LifecycleActivity a = activity(TEN, TWELVE, ActivityVisibility.PUBLIC);
        e.addActivity(a);
        List<LifecycleActivity> q = List.of(a);

        assertThat(EventSearchMatcher.matches(e, q, criteria("  100% java_b ", null, null, null, null), Set.of()))
                .isTrue();
        assertThat(EventSearchMatcher.matches(e, q, criteria("100x", null, null, null, null), Set.of())).isFalse();
        assertThat(EventSearchMatcher.matches(e, q, criteria("%", null, null, null, null), Set.of())).isTrue();
        assertThat(EventSearchMatcher.matches(e, q, criteria("java.basics", null, null, null, null), Set.of()))
                .isFalse();
        assertThat(EventSearchMatcher.matches(e, q, criteria("HANDS-ON", null, null, null, null), Set.of())).isTrue();
        assertThat(EventSearchMatcher.matches(e, q, criteria("   ", null, null, null, null), Set.of())).isTrue();
        assertThat(EventSearchMatcher.matches(e, q, criteria("robotics", null, null, null, null), Set.of(orgId)))
                .isTrue();
        assertThat(EventSearchMatcher.matches(e, q, criteria("robotics", null, null, null, null), Set.of(keywordOrg)))
                .isFalse();
    }

    @Test
    @DisplayName("US-25 the date range matches an overlapping activity")
    void us25_dateRangeMatchesOverlappingActivity() {
        LifecycleEvent e = event("Title", "Desc", EventCategory.WORKSHOP, orgId);
        LifecycleActivity a = activity(TEN, TWELVE, ActivityVisibility.PUBLIC);
        e.addActivity(a);
        List<LifecycleActivity> q = List.of(a);
        Instant eleven = Instant.parse("2031-01-01T11:00:00Z");
        Instant thirteen = Instant.parse("2031-01-01T13:00:00Z");
        Instant halfPastTwelve = Instant.parse("2031-01-01T12:30:00Z");

        assertThat(EventSearchMatcher.matches(e, q, criteria(null, eleven, thirteen, null, null), Set.of())).isTrue();
        assertThat(EventSearchMatcher.matches(e, q, criteria(null, halfPastTwelve, thirteen, null, null), Set.of()))
                .isFalse();
        assertThat(EventSearchMatcher.matches(e, q, criteria(null, eleven, null, null, null), Set.of())).isTrue();
        assertThat(EventSearchMatcher.matches(e, q, criteria(null, null, eleven, null, null), Set.of())).isTrue();
        assertThat(EventSearchMatcher.matches(e, q, criteria(null, null, Instant.parse("2031-01-01T09:30:00Z"), null, null), Set.of()))
                .isFalse();
    }

    @Test
    @DisplayName("US-25 filters combine with the keyword")
    void us25_filtersCombineWithKeyword() {
        LifecycleEvent e = event("Chess night", "Desc", EventCategory.SOCIAL, orgId);
        LifecycleActivity a = activity(TEN, TWELVE, ActivityVisibility.PUBLIC);
        e.addActivity(a);
        List<LifecycleActivity> q = List.of(a);

        assertThat(EventSearchMatcher.matches(e, q, criteria("chess", null, null, orgId, EventCategory.SOCIAL), Set.of()))
                .isTrue();
        assertThat(EventSearchMatcher.matches(e, q, criteria("chess", null, null, orgId, EventCategory.WORKSHOP), Set.of()))
                .isFalse();
        assertThat(EventSearchMatcher.matches(e, q, criteria("chess", null, null, UUID.randomUUID(), EventCategory.SOCIAL), Set.of()))
                .isFalse();
        assertThat(EventSearchMatcher.matches(e, q, criteria("poker", null, null, orgId, EventCategory.SOCIAL), Set.of()))
                .isFalse();
    }
}
