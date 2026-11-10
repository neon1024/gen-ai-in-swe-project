package com.ubb.genai.eventlifecycle.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ubb.genai.eventlifecycle.EventLifecycleFixtures;
import com.ubb.genai.eventlifecycle.MutableClock;
import com.ubb.genai.eventlifecycle.TestClockConfiguration;
import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.port.EventRole;
import com.ubb.genai.eventlifecycle.shared.port.EventTeamDirectory;
import com.ubb.genai.eventlifecycle.shared.port.OrganizationDirectory;
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
class FeedbackServiceTest {

    private static final Instant START = Instant.parse("2027-04-01T10:00:00Z");
    private static final Instant END = START.plus(Duration.ofHours(2));

    @Autowired
    private FeedbackService service;

    @Autowired
    private FeedbackRepository feedback;

    @Autowired
    private LifecycleEventRepository events;

    @Autowired
    private MutableClock clock;

    @MockitoBean
    private SignupPort signupPort;

    @MockitoBean
    private EventTeamDirectory team;

    @MockitoBean
    private OrganizationDirectory organizations;

    private final UUID user = UUID.randomUUID();
    private final List<UUID> created = new ArrayList<>();
    private LifecycleActivity activity;
    private LifecycleEvent event;

    @BeforeEach
    void setUp() {
        event = events.saveAndFlush(EventLifecycleFixtures.publishedEvent(
                UUID.randomUUID(), START, Duration.ofHours(2), ActivityVisibility.PUBLIC));
        created.add(event.getId());
        activity = events.findWithActivitiesById(event.getId()).orElseThrow().getActivities().get(0);
        clock.setInstant(END.plusSeconds(60));
        when(signupPort.wasMarkedPresent(activity.getId(), user)).thenReturn(true);
    }

    @AfterEach
    void cleanUp() {
        events.deleteAllById(created);
    }

    private GiveFeedbackRequest request(int rating, String comment) {
        return new GiveFeedbackRequest(rating, comment);
    }

    @Test
    @DisplayName("US-47 an attendee gives a rating with an optional comment")
    void us47_attendeeGivesRatingWithOptionalComment() {
        UUID id = service.give(activity.getId(), Optional.of(user), request(4, "  Great  "));

        Feedback stored = feedback.findById(id).orElseThrow();
        assertThat(stored.getRating()).isEqualTo(4);
        assertThat(stored.getComment()).isEqualTo("Great");
        assertThat(stored.getCreatedAt()).isEqualTo(clock.instant());

        UUID other = UUID.randomUUID();
        when(signupPort.wasMarkedPresent(activity.getId(), other)).thenReturn(true);
        UUID noComment = service.give(activity.getId(), Optional.of(other), request(5, null));
        assertThat(feedback.findById(noComment).orElseThrow().getComment()).isNull();

        UUID third = UUID.randomUUID();
        when(signupPort.wasMarkedPresent(activity.getId(), third)).thenReturn(true);
        UUID blank = service.give(activity.getId(), Optional.of(third), request(3, "   "));
        assertThat(feedback.findById(blank).orElseThrow().getComment()).isNull();
    }

    @Test
    @DisplayName("US-47 a participant can give feedback only once per activity")
    void us47_onlyOncePerActivity() {
        service.give(activity.getId(), Optional.of(user), request(4, null));

        assertThatThrownBy(() -> service.give(activity.getId(), Optional.of(user), request(2, "again")))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(p.code()).isEqualTo("FEEDBACK_ALREADY_GIVEN");
                });
    }

    @Test
    @DisplayName("US-47 a duplicate that slips past the pre-check is still answered with 409")
    void us47_uniqueConstraintViolationMapsToConflict() {
        service.give(activity.getId(), Optional.of(user), request(4, null));

        assertThatThrownBy(() -> service.persist(activity.getId(), user, request(1, null)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(p.code()).isEqualTo("FEEDBACK_ALREADY_GIVEN");
                });
    }

    @Test
    @DisplayName("BR-16 feedback is accepted on a completed event within the window")
    void br16_feedbackAllowedOnCompletedEvent() {
        LifecycleEvent loaded = events.findWithActivitiesById(event.getId()).orElseThrow();
        loaded.complete(END);
        events.saveAndFlush(loaded);
        assertThat(events.findById(event.getId()).orElseThrow().getState()).isEqualTo(EventState.COMPLETED);

        UUID id = service.give(activity.getId(), Optional.of(user), request(5, "Good"));

        assertThat(feedback.findById(id)).isPresent();
    }

    @Test
    @DisplayName("US-47 refusals map to 403 for not attended and 409 for the others")
    void us47_refusalsMapToStatuses() {
        UUID absent = UUID.randomUUID();
        assertThatThrownBy(() -> service.give(activity.getId(), Optional.of(absent), request(3, null)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(p.code()).isEqualTo("NOT_ATTENDED");
                });

        clock.setInstant(END.minusSeconds(1));
        assertThatThrownBy(() -> service.give(activity.getId(), Optional.of(user), request(3, null)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(p.code()).isEqualTo("FEEDBACK_NOT_OPEN");
                });
    }

    @Test
    @DisplayName("US-47 the eligibility check reports the reason")
    void us47_eligibilityEndpointReportsReason() {
        UUID absent = UUID.randomUUID();

        FeedbackEligibilityView refused = service.eligibility(activity.getId(), Optional.of(absent));
        assertThat(refused.eligible()).isFalse();
        assertThat(refused.reason()).isEqualTo("NOT_ATTENDED");

        FeedbackEligibilityView allowed = service.eligibility(activity.getId(), Optional.of(user));
        assertThat(allowed.eligible()).isTrue();
        assertThat(allowed.reason()).isNull();
    }

    @Test
    @DisplayName("US-47 an unknown activity returns 404")
    void us47_unknownActivityReturns404() {
        assertThatThrownBy(() -> service.give(UUID.randomUUID(), Optional.of(user), request(3, null)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(p.code()).isEqualTo("ACTIVITY_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("US-47 a guest cannot give feedback")
    void us47_guestCannotGiveFeedback() {
        assertThatThrownBy(() -> service.give(activity.getId(), Optional.empty(), request(3, null)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> assertThat(p.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> service.eligibility(activity.getId(), Optional.empty()))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> assertThat(p.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("BR-18 a stranger gets 404 from feedback eligibility and give on a private activity")
    void br18_strangerCannotProbePrivateActivity() {
        LifecycleEvent hidden = events.saveAndFlush(EventLifecycleFixtures.publishedEvent(
                UUID.randomUUID(), START, Duration.ofHours(2), ActivityVisibility.PRIVATE));
        created.add(hidden.getId());
        UUID privateId = events.findWithActivitiesById(hidden.getId()).orElseThrow().getActivities().get(0).getId();
        when(signupPort.wasMarkedPresent(privateId, user)).thenReturn(true);

        assertThatThrownBy(() -> service.eligibility(privateId, Optional.of(user)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(p.code()).isEqualTo("ACTIVITY_NOT_FOUND");
                });
        assertThatThrownBy(() -> service.give(privateId, Optional.of(user), request(3, null)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(p.code()).isEqualTo("ACTIVITY_NOT_FOUND");
                });

        when(organizations.isMember(hidden.getOrganizationId(), user)).thenReturn(true);
        assertThat(service.eligibility(privateId, Optional.of(user)).eligible()).isTrue();
    }

    @Test
    @DisplayName("BR-18 a stranger gets 404 from feedback on an activity of a draft event")
    void br18_strangerCannotProbeDraftActivity() {
        LifecycleEvent draft = EventLifecycleFixtures.draftEvent(UUID.randomUUID());
        draft.addActivity(LifecycleActivity.scheduled("Draft activity", START, END, ActivityVisibility.PUBLIC));
        draft = events.saveAndFlush(draft);
        created.add(draft.getId());
        UUID draftActivity = events.findWithActivitiesById(draft.getId()).orElseThrow().getActivities().get(0).getId();

        assertThatThrownBy(() -> service.eligibility(draftActivity, Optional.of(user)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> assertThat(p.code()).isEqualTo("ACTIVITY_NOT_FOUND"));
    }

    private void giveRated(int rating, String comment) {
        UUID attendee = UUID.randomUUID();
        when(signupPort.wasMarkedPresent(activity.getId(), attendee)).thenReturn(true);
        service.give(activity.getId(), Optional.of(attendee), request(rating, comment));
    }

    @Test
    @DisplayName("US-48 the team sees the average, distribution and comments of an activity")
    void us48_teamSeesSummary() {
        UUID editor = UUID.randomUUID();
        when(team.roleOf(event.getId(), editor)).thenReturn(Optional.of(EventRole.EDITOR));
        giveRated(5, "Great");
        giveRated(4, null);
        giveRated(4, "Fine");
        giveRated(1, null);

        FeedbackSummary summary = service.summary(activity.getId(), Optional.of(editor));

        assertThat(summary.activityId()).isEqualTo(activity.getId());
        assertThat(summary.activityTitle()).isEqualTo(activity.getTitle());
        assertThat(summary.count()).isEqualTo(4);
        assertThat(summary.averageRating()).isEqualTo(3.5);
        assertThat(summary.distribution().values()).containsExactly(1, 0, 0, 2, 1);
        assertThat(summary.comments()).extracting(FeedbackComment::comment).containsExactlyInAnyOrder("Great", "Fine");
    }

    @Test
    @DisplayName("US-48 staff can view feedback")
    void us48_staffCanViewFeedback() {
        UUID staff = UUID.randomUUID();
        when(team.roleOf(event.getId(), staff)).thenReturn(Optional.of(EventRole.STAFF));

        assertThat(service.summary(activity.getId(), Optional.of(staff)).count()).isZero();
    }

    @Test
    @DisplayName("US-48 an organization admin can view feedback")
    void us48_orgAdminCanViewFeedback() {
        UUID admin = UUID.randomUUID();
        when(organizations.isOrgAdmin(event.getOrganizationId(), admin)).thenReturn(true);

        assertThat(service.summary(activity.getId(), Optional.of(admin)).averageRating()).isNull();
    }

    @Test
    @DisplayName("US-48 a user outside the event team is refused with 403")
    void us48_nonTeamUserIsRefused() {
        assertThatThrownBy(() -> service.summary(activity.getId(), Optional.of(user)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(p.code()).isEqualTo("TEAM_MEMBER_REQUIRED");
                });
    }

    @Test
    @DisplayName("US-48 a guest gets 401 and an unknown activity gets 404")
    void us48_guestAndUnknownActivity() {
        assertThatThrownBy(() -> service.summary(activity.getId(), Optional.empty()))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> assertThat(p.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> service.summary(UUID.randomUUID(), Optional.of(user)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(p.code()).isEqualTo("ACTIVITY_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("BR-03 the summary of a draft event is hidden from non-team users with 404")
    void br03_draftEventSummaryIsNotFoundForNonTeam() {
        LifecycleEvent draft = events.saveAndFlush(EventLifecycleFixtures.draftEvent(UUID.randomUUID()));
        draft.addActivity(LifecycleActivity.scheduled("Draft activity", START, END, ActivityVisibility.PUBLIC));
        events.saveAndFlush(draft);
        created.add(draft.getId());
        UUID draftActivity = events.findWithActivitiesById(draft.getId()).orElseThrow().getActivities().get(0).getId();

        assertThatThrownBy(() -> service.summary(draftActivity, Optional.of(user)))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(p.code()).isEqualTo("EVENT_NOT_FOUND");
                });
    }
}
