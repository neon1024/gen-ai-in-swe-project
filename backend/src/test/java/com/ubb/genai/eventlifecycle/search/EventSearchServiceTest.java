package com.ubb.genai.eventlifecycle.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ubb.genai.eventlifecycle.MutableClock;
import com.ubb.genai.eventlifecycle.TestClockConfiguration;
import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.EventCategory;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.port.OrganizationDirectory;
import com.ubb.genai.eventlifecycle.shared.port.PrivateActivityInvitations;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The shared test database keeps published events left by other test classes, some of them in the future of this
 * class's clock. Every test therefore uses fresh random organization ids and restricts the search to them with the
 * organization filter (except where the organization filter itself is under test, which asserts only on its own
 * organizations). Created events are deleted after each test.
 */
@SpringBootTest
@Import(TestClockConfiguration.class)
class EventSearchServiceTest {

    private static final Instant NOW = Instant.parse("2031-01-01T00:00:00Z");

    @Autowired
    private EventSearchService service;

    @Autowired
    private LifecycleEventRepository events;

    @Autowired
    private MutableClock clock;

    @MockitoBean
    private OrganizationDirectory organizations;

    @MockitoBean
    private PrivateActivityInvitations invitations;

    private final UUID orgId = UUID.randomUUID();
    private final List<UUID> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clock.setInstant(NOW);
        when(organizations.organizationName(any())).thenReturn(Optional.empty());
        when(organizations.organizationIdsWithNameContaining(any())).thenReturn(Set.of());
    }

    @AfterEach
    void cleanUp() {
        events.deleteAllById(created);
    }

    private LifecycleEvent save(UUID org, String title, boolean publish, ActivityVisibility visibility, Duration... startsIn) {
        LifecycleEvent event = LifecycleEvent.draft(org, title, "Description of " + title, EventCategory.WORKSHOP);
        for (Duration offset : startsIn) {
            event.addActivity(LifecycleActivity.scheduled(
                    "Activity", NOW.plus(offset), NOW.plus(offset).plus(Duration.ofHours(1)), visibility));
        }
        if (publish) {
            event.publish(NOW.minus(Duration.ofDays(1)));
        }
        LifecycleEvent saved = events.saveAndFlush(event);
        created.add(saved.getId());
        return saved;
    }

    private LifecycleEvent published(String title, Duration... startsIn) {
        return save(orgId, title, true, ActivityVisibility.PUBLIC, startsIn);
    }

    private EventSearchCriteria forOrg(UUID org) {
        return new EventSearchCriteria(null, null, null, org, null, 0, 20);
    }

    private List<String> titles(EventSearchResult result) {
        return result.items().stream().map(EventSearchResult.EventSummary::title).toList();
    }

    @Test
    @DisplayName("US-24 lists upcoming published events soonest first")
    void us24_listsUpcomingPublishedEventsSoonestFirst() {
        published("In three days", Duration.ofDays(3));
        published("In one day", Duration.ofDays(1));
        published("In two days", Duration.ofDays(2));
        LifecycleEvent completed = published("Completed", Duration.ofDays(4));
        LifecycleEvent toComplete = events.findWithActivitiesById(completed.getId()).orElseThrow();
        toComplete.complete(NOW);
        events.saveAndFlush(toComplete);
        published("Past", Duration.ofDays(-3));

        EventSearchResult result = service.search(forOrg(orgId), Optional.empty());

        assertThat(titles(result)).containsExactly("In one day", "In two days", "In three days");
        assertThat(result.totalItems()).isEqualTo(3);
        assertThat(result.items().get(0).nextActivityStartsAt()).isEqualTo(NOW.plus(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("BR-03 draft events never appear")
    void br03_draftEventsNeverAppear() {
        UUID admin = UUID.randomUUID();
        when(organizations.isOrgAdmin(orgId, admin)).thenReturn(true);
        save(orgId, "Draft", false, ActivityVisibility.PUBLIC, Duration.ofDays(1));

        assertThat(service.search(forOrg(orgId), Optional.of(admin)).items()).isEmpty();
        assertThat(service.search(forOrg(orgId), Optional.empty()).items()).isEmpty();
    }

    @Test
    @DisplayName("BR-18 a private-only event is hidden from guests and visible to members and invitees")
    void br18_privateOnlyEventHiddenFromGuestsVisibleToMembers() {
        LifecycleEvent event = save(orgId, "Private", true, ActivityVisibility.PRIVATE, Duration.ofDays(1));
        UUID member = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        when(organizations.isMember(orgId, member)).thenReturn(true);
        when(invitations.isInvited(event.getActivities().get(0).getId(), invitee)).thenReturn(true);

        assertThat(service.search(forOrg(orgId), Optional.empty()).items()).isEmpty();
        assertThat(titles(service.search(forOrg(orgId), Optional.of(member)))).containsExactly("Private");
        assertThat(titles(service.search(forOrg(orgId), Optional.of(invitee)))).containsExactly("Private");
        assertThat(service.search(forOrg(orgId), Optional.of(UUID.randomUUID())).items()).isEmpty();
    }

    @Test
    @DisplayName("BR-18 private activities do not affect sort or date filter for guests")
    void br18_privateActivitiesDoNotAffectSortOrDateFilterForGuests() {
        LifecycleEvent mixed = LifecycleEvent.draft(orgId, "Mixed", "Mixed event", EventCategory.WORKSHOP);
        mixed.addActivity(LifecycleActivity.scheduled(
                "Private", NOW.plus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(1)).plus(Duration.ofHours(1)),
                ActivityVisibility.PRIVATE));
        mixed.addActivity(LifecycleActivity.scheduled(
                "Public", NOW.plus(Duration.ofDays(5)), NOW.plus(Duration.ofDays(5)).plus(Duration.ofHours(1)),
                ActivityVisibility.PUBLIC));
        mixed.publish(NOW.minus(Duration.ofDays(1)));
        created.add(events.saveAndFlush(mixed).getId());

        EventSearchResult all = service.search(forOrg(orgId), Optional.empty());
        assertThat(all.items()).hasSize(1);
        assertThat(all.items().get(0).nextActivityStartsAt()).isEqualTo(NOW.plus(Duration.ofDays(5)));

        EventSearchCriteria onlyTomorrow = new EventSearchCriteria(
                null, NOW.plus(Duration.ofHours(23)), NOW.plus(Duration.ofHours(27)), orgId, null, 0, 20);
        assertThat(service.search(onlyTomorrow, Optional.empty()).items()).isEmpty();
    }

    @Test
    @DisplayName("US-24 the keyword matches the organization name")
    void us24_keywordMatchesOrganizationName() {
        when(organizations.organizationIdsWithNameContaining("Robotics")).thenReturn(Set.of(orgId));
        when(organizations.organizationName(orgId)).thenReturn(Optional.of("Robotics Club"));
        published("Soldering basics", Duration.ofDays(1));

        EventSearchCriteria criteria = new EventSearchCriteria(" Robotics ", null, null, orgId, null, 0, 20);
        EventSearchResult result = service.search(criteria, Optional.empty());

        assertThat(titles(result)).containsExactly("Soldering basics");
        assertThat(result.items().get(0).organizationName()).isEqualTo("Robotics Club");
    }

    @Test
    @DisplayName("US-25 pagination reports the total")
    void us25_paginationReportsTotal() {
        for (int i = 0; i < 25; i++) {
            published("Event %02d".formatted(i), Duration.ofHours(1 + i));
        }

        EventSearchResult result = service.search(
                new EventSearchCriteria(null, null, null, orgId, null, 1, 10), Optional.empty());

        assertThat(result.items()).hasSize(10);
        assertThat(result.totalItems()).isEqualTo(25);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.items().get(0).title()).isEqualTo("Event 10");
    }

    @Test
    @DisplayName("US-25 organization options ignore the organization filter")
    void us25_organizationOptionsIgnoreOrganizationFilter() {
        UUID otherOrg = UUID.randomUUID();
        when(organizations.organizationName(orgId)).thenReturn(Optional.of("Alpha Club"));
        when(organizations.organizationName(otherOrg)).thenReturn(Optional.of("Beta Club"));
        published("Alpha event", Duration.ofDays(1));
        save(otherOrg, "Beta event", true, ActivityVisibility.PUBLIC, Duration.ofDays(1));

        EventSearchResult result = service.search(forOrg(orgId), Optional.empty());

        assertThat(titles(result)).containsExactly("Alpha event");
        assertThat(result.organizations())
                .contains(
                        new EventSearchResult.OrganizationOption(orgId, "Alpha Club"),
                        new EventSearchResult.OrganizationOption(otherOrg, "Beta Club"));
        assertThat(result.organizations())
                .extracting(EventSearchResult.OrganizationOption::name)
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }
}
