package com.ubb.genai.eventlifecycle.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ubb.genai.eventlifecycle.EventLifecycleFixtures;
import com.ubb.genai.eventlifecycle.TestClockConfiguration;
import com.ubb.genai.eventlifecycle.feedback.Feedback;
import com.ubb.genai.eventlifecycle.feedback.FeedbackRepository;
import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.port.EventRole;
import com.ubb.genai.eventlifecycle.shared.port.EventTeamDirectory;
import com.ubb.genai.eventlifecycle.shared.port.OrganizationDirectory;
import com.ubb.genai.eventlifecycle.shared.port.SignupFigures;
import com.ubb.genai.eventlifecycle.shared.port.SignupPort;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestClockConfiguration.class)
class EventReportServiceTest {

    private static final Instant START = Instant.parse("2027-04-01T10:00:00Z");

    @Autowired
    private EventReportService service;

    @Autowired
    private FeedbackRepository feedback;

    @Autowired
    private LifecycleEventRepository events;

    @MockitoBean
    private SignupPort signupPort;

    @MockitoBean
    private EventTeamDirectory team;

    @MockitoBean
    private OrganizationDirectory organizations;

    private final UUID staff = UUID.randomUUID();
    private final UUID outsider = UUID.randomUUID();
    private final List<UUID> created = new ArrayList<>();
    private LifecycleEvent event;
    private List<LifecycleActivity> activities;

    @BeforeEach
    void setUp() {
        event = events.saveAndFlush(EventLifecycleFixtures.publishedEvent(
                UUID.randomUUID(), START, Duration.ofHours(1),
                ActivityVisibility.PUBLIC, ActivityVisibility.PRIVATE, ActivityVisibility.PUBLIC));
        created.add(event.getId());
        activities = events.findWithActivitiesById(event.getId()).orElseThrow().getActivities();
        when(team.roleOf(event.getId(), staff)).thenReturn(Optional.of(EventRole.STAFF));
    }

    @AfterEach
    void cleanUp() {
        feedback.deleteAll(feedback.findAll().stream()
                .filter(f -> activities.stream().anyMatch(a -> a.getId().equals(f.getActivityId())))
                .toList());
        events.deleteAllById(created);
    }

    private LifecycleActivity byTitle(String title) {
        return activities.stream().filter(a -> a.getTitle().equals(title)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("US-49 the report shows the figures of every activity")
    void us49_reportShowsFiguresPerActivity() {
        LifecycleActivity first = byTitle("Activity 1");
        LifecycleActivity second = byTitle("Activity 2");
        when(signupPort.figures(first.getId())).thenReturn(new SignupFigures(1, 2, 3, 3));
        when(signupPort.figures(second.getId())).thenReturn(new SignupFigures(0, 0, 0, 0));
        when(signupPort.figures(byTitle("Activity 3").getId())).thenReturn(new SignupFigures(5, 0, 0, 0));
        feedback.saveAll(List.of(
                new Feedback(first.getId(), UUID.randomUUID(), 5, null, START),
                new Feedback(first.getId(), UUID.randomUUID(), 4, "ok", START),
                new Feedback(first.getId(), UUID.randomUUID(), 4, null, START)));

        EventReport report = service.report(event.getId(), Optional.of(staff));

        assertThat(report.eventId()).isEqualTo(event.getId());
        assertThat(report.eventTitle()).isEqualTo("Sample event");
        assertThat(report.eventState()).isEqualTo(EventState.PUBLISHED);
        assertThat(report.activities()).extracting(ActivityReportRow::title)
                .containsExactly("Activity 1", "Activity 2", "Activity 3");
        ActivityReportRow row = report.activities().get(0);
        assertThat(row.confirmedSignups()).isEqualTo(4);
        assertThat(row.waitlistSize()).isEqualTo(2);
        assertThat(row.cancellations()).isEqualTo(3);
        assertThat(row.attendanceRate()).isEqualTo(0.75);
        assertThat(row.ratingCount()).isEqualTo(3);
        assertThat(row.averageRating()).isEqualTo(4.33);
        ActivityReportRow empty = report.activities().get(1);
        assertThat(empty.attendanceRate()).isNull();
        assertThat(empty.averageRating()).isNull();
        assertThat(empty.ratingCount()).isZero();
        assertThat(report.activities().get(2).attendanceRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("US-49 event staff can view the report of a completed event")
    void us49_staffCanViewReport() {
        LifecycleEvent loaded = events.findById(event.getId()).orElseThrow();
        loaded.complete(START.plus(Duration.ofDays(1)));
        events.saveAndFlush(loaded);
        when(signupPort.figures(any())).thenReturn(new SignupFigures(0, 0, 0, 0));

        EventReport report = service.report(event.getId(), Optional.of(staff));

        assertThat(report.eventState()).isEqualTo(EventState.COMPLETED);
        assertThat(report.activities()).hasSize(3);
    }

    @Test
    @DisplayName("US-49 an organization admin can view the report")
    void us49_orgAdminCanViewReport() {
        UUID admin = UUID.randomUUID();
        when(organizations.isOrgAdmin(event.getOrganizationId(), admin)).thenReturn(true);
        when(signupPort.figures(any())).thenReturn(new SignupFigures(0, 0, 0, 0));

        assertThat(service.report(event.getId(), Optional.of(admin)).activities()).hasSize(3);
    }

    @Test
    @DisplayName("US-49 a user outside the event team is refused")
    void us49_nonTeamUserIsRefused() {
        assertThatThrownBy(() -> service.report(event.getId(), Optional.of(outsider)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(p.code()).isEqualTo("TEAM_MEMBER_REQUIRED");
                });
    }

    @Test
    @DisplayName("US-49 a guest gets 401 and an unknown event gets 404")
    void us49_guestAndUnknownEventAreRefused() {
        assertThatThrownBy(() -> service.report(event.getId(), Optional.empty()))
                .isInstanceOfSatisfying(LifecycleProblem.class,
                        p -> assertThat(p.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> service.report(UUID.randomUUID(), Optional.of(staff)))
                .isInstanceOfSatisfying(LifecycleProblem.class,
                        p -> assertThat(p.status()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("BR-03 a draft event is hidden from a non-team user as 404")
    void br03_draftEventIsHiddenFromNonTeamUser() {
        LifecycleEvent draft = events.saveAndFlush(EventLifecycleFixtures.draftEvent(UUID.randomUUID()));
        created.add(draft.getId());

        assertThatThrownBy(() -> service.report(draft.getId(), Optional.of(outsider)))
                .isInstanceOfSatisfying(LifecycleProblem.class,
                        p -> assertThat(p.status()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("US-50 rows are ordered by start time with nulls last, then by title")
    void us50_rowsAreOrderedByStartThenTitle() {
        UUID orgId = UUID.randomUUID();
        LifecycleEvent ordered = EventLifecycleFixtures.draftEvent(orgId);
        ordered.addActivity(LifecycleActivity.scheduled("B", START, START.plusSeconds(60), ActivityVisibility.PUBLIC));
        ordered.addActivity(LifecycleActivity.scheduled("A", START, START.plusSeconds(60), ActivityVisibility.PUBLIC));
        ordered.addActivity(LifecycleActivity.scheduled("Early", START.minusSeconds(60), START, ActivityVisibility.PUBLIC));
        ordered = events.saveAndFlush(ordered);
        created.add(ordered.getId());
        when(team.roleOf(ordered.getId(), staff)).thenReturn(Optional.of(EventRole.STAFF));
        when(signupPort.figures(any())).thenReturn(new SignupFigures(0, 0, 0, 0));

        assertThat(service.report(ordered.getId(), Optional.of(staff)).activities())
                .extracting(ActivityReportRow::title)
                .containsExactly("Early", "A", "B");
    }
}
