package com.ubb.genai.eventlifecycle.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ubb.genai.eventlifecycle.EventLifecycleFixtures;
import com.ubb.genai.eventlifecycle.TestClockConfiguration;
import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleAgendaItem;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleActivityRepository;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.port.EventRole;
import com.ubb.genai.eventlifecycle.shared.port.EventTeamDirectory;
import com.ubb.genai.eventlifecycle.shared.port.OrganizationDirectory;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestClockConfiguration.class)
class DraftDeletionServiceTest {

    private static final Instant START = Instant.parse("2027-02-01T10:00:00Z");

    @Autowired
    private DraftDeletionService service;

    @Autowired
    private LifecycleEventRepository events;

    @Autowired
    private LifecycleActivityRepository activities;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private EventTeamDirectory team;

    @MockitoBean
    private OrganizationDirectory organizations;

    private final UUID orgId = UUID.randomUUID();
    private final UUID owner = UUID.randomUUID();
    private final UUID editor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(team.roleOf(any(), any())).thenReturn(Optional.empty());
        when(organizations.isOrgAdmin(any(), any())).thenReturn(false);
    }

    private LifecycleEvent save(LifecycleEvent event) {
        LifecycleEvent saved = events.saveAndFlush(event);
        when(team.roleOf(saved.getId(), owner)).thenReturn(Optional.of(EventRole.OWNER));
        when(team.roleOf(saved.getId(), editor)).thenReturn(Optional.of(EventRole.EDITOR));
        return saved;
    }

    private static void assertProblem(Runnable action, HttpStatus status, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(status);
                    assertThat(p.code()).isEqualTo(code);
                });
    }

    private int agendaItemsOf(UUID eventId) {
        return jdbc.queryForObject(
                "select count(*) from lifecycle_agenda_item i join lifecycle_activity a on a.id = i.activity_id"
                        + " where a.event_id = ?",
                Integer.class, eventId);
    }

    @Test
    @DisplayName("US-22 a draft is deleted together with its activities and agenda items")
    void us22_draftIsDeletedWithActivitiesAndAgendaItems() {
        LifecycleEvent draft = EventLifecycleFixtures.draftEvent(orgId);
        LifecycleActivity first = LifecycleActivity.scheduled(
                "Activity 1", START, START.plus(Duration.ofHours(1)), ActivityVisibility.PUBLIC);
        LifecycleActivity second = LifecycleActivity.scheduled(
                "Activity 2", START, START.plus(Duration.ofHours(1)), ActivityVisibility.PUBLIC);
        draft.addActivity(first);
        draft.addActivity(second);
        new LifecycleAgendaItem(first, START, "Opening", "Ada");
        new LifecycleAgendaItem(first, START.plus(Duration.ofMinutes(30)), "Talk", "Grace");
        new LifecycleAgendaItem(second, START, "Panel", null);
        UUID id = save(draft).getId();
        assertThat(agendaItemsOf(id)).isEqualTo(3);

        service.deleteDraft(id, Optional.of(owner));

        assertThat(jdbc.queryForObject("select count(*) from lifecycle_event where id = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from lifecycle_activity where event_id = ?", Integer.class, id))
                .isZero();
        assertThat(activities.existsById(first.getId())).isFalse();
        assertThat(activities.existsById(second.getId())).isFalse();
        assertThat(jdbc.queryForObject(
                        "select count(*) from lifecycle_agenda_item where activity_id in (?, ?)",
                        Integer.class, first.getId(), second.getId()))
                .isZero();
    }

    @Test
    @DisplayName("US-22 a published event cannot be deleted")
    void us22_publishedEventCannotBeDeleted() {
        UUID id = save(EventLifecycleFixtures.publishedEvent(
                        orgId, START, Duration.ofHours(1), ActivityVisibility.PUBLIC))
                .getId();

        assertProblem(() -> service.deleteDraft(id, Optional.of(owner)), HttpStatus.CONFLICT, "EVENT_NOT_DRAFT");
        assertThatThrownBy(() -> service.deleteDraft(id, Optional.of(owner)))
                .hasMessage("Published events cannot be deleted, only cancelled.");
        assertThat(events.existsById(id)).isTrue();
    }

    @Test
    @DisplayName("BR-05 cancelled and completed events cannot be deleted")
    void br05_cancelledAndCompletedEventsCannotBeDeleted() {
        LifecycleEvent cancelled = EventLifecycleFixtures.publishedEvent(
                orgId, START, Duration.ofHours(1), ActivityVisibility.PUBLIC);
        cancelled.cancel(START);
        LifecycleEvent completed = EventLifecycleFixtures.publishedEvent(
                orgId, START, Duration.ofHours(1), ActivityVisibility.PUBLIC);
        completed.complete(START);
        UUID cancelledId = save(cancelled).getId();
        UUID completedId = save(completed).getId();

        assertProblem(() -> service.deleteDraft(cancelledId, Optional.of(owner)), HttpStatus.CONFLICT, "EVENT_NOT_DRAFT");
        assertProblem(() -> service.deleteDraft(completedId, Optional.of(owner)), HttpStatus.CONFLICT, "EVENT_NOT_DRAFT");
        assertThat(events.existsById(cancelledId)).isTrue();
        assertThat(events.existsById(completedId)).isTrue();
    }

    @Test
    @DisplayName("US-22 only owner rights may delete a draft")
    void us22_onlyOwnerRightsMayDelete() {
        UUID id = save(EventLifecycleFixtures.draftEvent(orgId)).getId();

        assertProblem(() -> service.deleteDraft(id, Optional.of(editor)), HttpStatus.FORBIDDEN, "OWNER_RIGHTS_REQUIRED");
        assertThat(events.existsById(id)).isTrue();
    }

    @Test
    @DisplayName("US-22 a draft is hidden from non-team users and a missing event is not found")
    void us22_hiddenOrMissingDraftIsNotFound() {
        UUID id = save(EventLifecycleFixtures.draftEvent(orgId)).getId();

        assertProblem(() -> service.deleteDraft(id, Optional.of(UUID.randomUUID())), HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND");
        assertProblem(() -> service.deleteDraft(UUID.randomUUID(), Optional.of(owner)), HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND");
        assertThat(events.existsById(id)).isTrue();
    }
}
