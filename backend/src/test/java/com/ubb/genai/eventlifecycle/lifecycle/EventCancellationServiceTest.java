package com.ubb.genai.eventlifecycle.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ubb.genai.eventlifecycle.EventLifecycleFixtures;
import com.ubb.genai.eventlifecycle.MutableClock;
import com.ubb.genai.eventlifecycle.TestClockConfiguration;
import com.ubb.genai.eventlifecycle.shared.model.ActivityState;
import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.port.EventRole;
import com.ubb.genai.eventlifecycle.shared.port.EventTeamDirectory;
import com.ubb.genai.eventlifecycle.shared.port.NotificationPort;
import com.ubb.genai.eventlifecycle.shared.port.OrganizationDirectory;
import com.ubb.genai.eventlifecycle.shared.port.SignupPort;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestClockConfiguration.class)
class EventCancellationServiceTest {

    private static final Instant START = Instant.parse("2027-02-01T10:00:00Z");

    @Autowired
    private EventCancellationService service;

    @Autowired
    private LifecycleEventRepository events;

    @Autowired
    private MutableClock clock;

    @Autowired
    private TransactionTemplate tx;

    @MockitoBean
    private SignupPort signupPort;

    @MockitoBean
    private NotificationPort notificationPort;

    @MockitoBean
    private EventTeamDirectory team;

    @MockitoBean
    private OrganizationDirectory organizations;

    private final UUID orgId = UUID.randomUUID();
    private final UUID owner = UUID.randomUUID();
    private final UUID editor = UUID.randomUUID();
    private final UUID staff = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        clock.setInstant(Instant.parse("2027-01-01T00:00:00Z"));
        when(team.roleOf(any(), any())).thenReturn(Optional.empty());
        when(organizations.isOrgAdmin(any(), any())).thenReturn(false);
        when(signupPort.cancelAllSignups(any())).thenReturn(Set.of());
    }

    private LifecycleEvent savePublished(int activities) {
        ActivityVisibility[] visibilities = new ActivityVisibility[activities];
        java.util.Arrays.fill(visibilities, ActivityVisibility.PUBLIC);
        LifecycleEvent event = EventLifecycleFixtures.publishedEvent(
                orgId, START, Duration.ofHours(1), visibilities);
        return save(event);
    }

    private LifecycleEvent save(LifecycleEvent event) {
        LifecycleEvent saved = events.saveAndFlush(event);
        when(team.roleOf(saved.getId(), owner)).thenReturn(Optional.of(EventRole.OWNER));
        when(team.roleOf(saved.getId(), editor)).thenReturn(Optional.of(EventRole.EDITOR));
        when(team.roleOf(saved.getId(), staff)).thenReturn(Optional.of(EventRole.STAFF));
        return saved;
    }

    private LifecycleEvent reload(UUID id) {
        return events.findWithActivitiesById(id).orElseThrow();
    }

    private static void assertProblem(Runnable action, HttpStatus status, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(status);
                    assertThat(p.code()).isEqualTo(code);
                });
    }

    @Test
    @DisplayName("US-20 cancelling an activity cancels its signups and waitlist and notifies the recipients")
    void us20_cancellingActivityCancelsItsSignupsAndWaitlist() {
        LifecycleEvent event = savePublished(2);
        UUID a = event.getActivities().get(0).getId();
        UUID b = event.getActivities().get(1).getId();
        Set<UUID> recipients = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(signupPort.cancelAllSignups(a)).thenReturn(recipients);

        LifecycleEventView view = service.cancelActivity(a, Optional.of(owner));

        LifecycleEvent reloaded = reload(event.getId());
        assertThat(reloaded.getState()).isEqualTo(EventState.PUBLISHED);
        assertThat(reloaded.getActivities()).filteredOn(x -> x.getId().equals(a))
                .singleElement().extracting(x -> x.getState()).isEqualTo(ActivityState.CANCELLED);
        assertThat(reloaded.getActivities()).filteredOn(x -> x.getId().equals(b))
                .singleElement().extracting(x -> x.getState()).isEqualTo(ActivityState.SCHEDULED);
        assertThat(view.state()).isEqualTo(EventState.PUBLISHED);
        verify(signupPort).cancelAllSignups(a);
        verify(notificationPort).activityCancelled(a, recipients);
        verify(notificationPort, never()).eventCancelled(any(), any());
    }

    @Test
    @DisplayName("BR-04 cancelling the last scheduled activity cancels the event")
    void br04_cancellingLastScheduledActivityCancelsEvent() {
        LifecycleEvent event = savePublished(1);
        UUID a = event.getActivities().get(0).getId();
        Set<UUID> recipients = Set.of(UUID.randomUUID());
        when(signupPort.cancelAllSignups(a)).thenReturn(recipients);

        LifecycleEventView view = service.cancelActivity(a, Optional.of(owner));

        assertThat(reload(event.getId()).getState()).isEqualTo(EventState.CANCELLED);
        assertThat(view.state()).isEqualTo(EventState.CANCELLED);
        verify(notificationPort).activityCancelled(a, recipients);
        verify(notificationPort).eventCancelled(event.getId(), recipients);
    }

    @Test
    @DisplayName("US-20 editors and staff cannot cancel an activity")
    void us20_editorsAndStaffCannotCancelActivity() {
        LifecycleEvent event = savePublished(1);
        UUID a = event.getActivities().get(0).getId();

        assertProblem(() -> service.cancelActivity(a, Optional.of(editor)), HttpStatus.FORBIDDEN, "OWNER_RIGHTS_REQUIRED");
        assertProblem(() -> service.cancelActivity(a, Optional.of(staff)), HttpStatus.FORBIDDEN, "OWNER_RIGHTS_REQUIRED");
        verifyNoInteractions(signupPort);
        verifyNoInteractions(notificationPort);
    }

    @Test
    @DisplayName("US-20 an unknown activity is answered as not found")
    void us20_unknownActivityIsNotFound() {
        assertProblem(() -> service.cancelActivity(UUID.randomUUID(), Optional.of(owner)),
                HttpStatus.NOT_FOUND, "ACTIVITY_NOT_FOUND");
    }

    @Test
    @DisplayName("US-20 cancelling an activity of a completed event is refused")
    void us20_cancellingActivityOfReadOnlyEventIsRefused() {
        LifecycleEvent event = savePublished(1);
        event.complete(START);
        events.saveAndFlush(event);

        assertProblem(() -> service.cancelActivity(event.getActivities().get(0).getId(), Optional.of(owner)),
                HttpStatus.CONFLICT, "EVENT_READ_ONLY");
        verifyNoInteractions(signupPort);
    }

    @Test
    @DisplayName("US-20 cancelling an ended activity is refused")
    void us20_cancellingEndedActivityIsRefused() {
        LifecycleEvent event = savePublished(1);
        UUID a = event.getActivities().get(0).getId();
        clock.setInstant(START.plus(Duration.ofHours(1)));

        assertProblem(() -> service.cancelActivity(a, Optional.of(owner)), HttpStatus.CONFLICT, "ACTIVITY_ENDED");
        verifyNoInteractions(signupPort);
        verifyNoInteractions(notificationPort);
    }

    @Test
    @DisplayName("US-20 cancelling a cancelled activity is refused and does not notify again")
    void us20_cancellingCancelledActivityIsRefused() {
        LifecycleEvent event = savePublished(2);
        UUID a = event.getActivities().get(0).getId();
        service.cancelActivity(a, Optional.of(owner));
        reset(signupPort, notificationPort);

        assertProblem(() -> service.cancelActivity(a, Optional.of(owner)),
                HttpStatus.CONFLICT, "ACTIVITY_ALREADY_CANCELLED");
        verifyNoInteractions(signupPort);
        verifyNoInteractions(notificationPort);
    }

    @Test
    @DisplayName("US-21 cancelling an event cancels all scheduled activities and their signups")
    void us21_cancellingEventCancelsAllActivitiesAndSignups() {
        LifecycleEvent event = savePublished(3);
        UUID a = event.getActivities().get(0).getId();
        UUID b = event.getActivities().get(1).getId();
        UUID c = event.getActivities().get(2).getId();
        service.cancelActivity(a, Optional.of(owner));
        reset(signupPort, notificationPort);
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();
        when(signupPort.cancelAllSignups(b)).thenReturn(Set.of(u1, u2));
        when(signupPort.cancelAllSignups(c)).thenReturn(Set.of(u2, u3));

        LifecycleEventView view = service.cancelEvent(event.getId(), Optional.of(owner));

        LifecycleEvent reloaded = reload(event.getId());
        assertThat(reloaded.getState()).isEqualTo(EventState.CANCELLED);
        assertThat(reloaded.getActivities()).allMatch(x -> x.getState() == ActivityState.CANCELLED);
        assertThat(view.state()).isEqualTo(EventState.CANCELLED);
        verify(signupPort).cancelAllSignups(b);
        verify(signupPort).cancelAllSignups(c);
        verify(signupPort, never()).cancelAllSignups(a);
        verify(notificationPort, times(1)).eventCancelled(event.getId(), Set.of(u1, u2, u3));
    }

    @Test
    @DisplayName("US-21 a draft event can be cancelled")
    void us21_draftEventCanBeCancelled() {
        LifecycleEvent draft = save(EventLifecycleFixtures.draftEvent(orgId));

        LifecycleEventView view = service.cancelEvent(draft.getId(), Optional.of(owner));

        assertThat(view.state()).isEqualTo(EventState.CANCELLED);
        assertThat(reload(draft.getId()).getState()).isEqualTo(EventState.CANCELLED);
    }

    @Test
    @DisplayName("BR-06 a cancelled event cannot be cancelled again")
    void br06_cancellingCancelledEventIsRefused() {
        LifecycleEvent event = savePublished(1);
        service.cancelEvent(event.getId(), Optional.of(owner));
        reset(signupPort, notificationPort);

        assertProblem(() -> service.cancelEvent(event.getId(), Optional.of(owner)),
                HttpStatus.CONFLICT, "EVENT_NOT_CANCELLABLE");
        verifyNoInteractions(signupPort);
        verifyNoInteractions(notificationPort);
    }

    @Test
    @DisplayName("BR-06 a completed event cannot be cancelled")
    void br06_cancellingCompletedEventIsRefused() {
        LifecycleEvent event = savePublished(1);
        event.complete(START);
        events.saveAndFlush(event);

        assertProblem(() -> service.cancelEvent(event.getId(), Optional.of(owner)),
                HttpStatus.CONFLICT, "EVENT_NOT_CANCELLABLE");
        verifyNoInteractions(signupPort);
    }

    @Test
    @DisplayName("BR-03 cancelling a draft event as a stranger answers not found")
    void br03_cancellingHiddenDraftAnswersNotFound() {
        LifecycleEvent draft = save(EventLifecycleFixtures.draftEvent(orgId));

        assertProblem(() -> service.cancelEvent(draft.getId(), Optional.of(stranger)),
                HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND");
        assertProblem(() -> service.cancelEvent(draft.getId(), Optional.empty()),
                HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND");
    }

    @Test
    @DisplayName("US-21 cancelling without a user is unauthenticated")
    void us21_cancellingAsGuestOfVisibleEventIsUnauthenticated() {
        LifecycleEvent event = savePublished(1);

        assertProblem(() -> service.cancelEvent(event.getId(), Optional.empty()),
                HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
    }

    @Test
    @DisplayName("US-21 the view reports cancel and delete permissions")
    void us21_viewReportsPermissions() {
        LifecycleEvent published = savePublished(1);
        LifecycleEvent draft = save(EventLifecycleFixtures.draftEvent(orgId));

        assertThat(service.view(published.getId(), Optional.of(owner)).permissions())
                .isEqualTo(new LifecycleEventView.Permissions(true, false));
        assertThat(service.view(draft.getId(), Optional.of(owner)).permissions())
                .isEqualTo(new LifecycleEventView.Permissions(true, true));
        assertThat(service.view(published.getId(), Optional.of(editor)).permissions())
                .isEqualTo(new LifecycleEventView.Permissions(false, false));
        assertThat(service.view(published.getId(), Optional.empty()).permissions())
                .isEqualTo(new LifecycleEventView.Permissions(false, false));
        assertProblem(() -> service.view(draft.getId(), Optional.of(stranger)), HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND");
    }

    @Test
    @DisplayName("US-20 cancelling an activity without the cascade increments the event version")
    void us20_cancellingActivityIncrementsEventVersion() {
        LifecycleEvent event = savePublished(2);
        long before = reload(event.getId()).getVersion();

        service.cancelActivity(event.getActivities().get(0).getId(), Optional.of(owner));

        assertThat(reload(event.getId()).getVersion()).isGreaterThan(before);
    }

    @Test
    @DisplayName("US-20 a stale concurrent activity cancellation is rejected")
    void us20_concurrentActivityCancellationIsRejected() throws Exception {
        LifecycleEvent event = savePublished(2);
        UUID a = event.getActivities().get(0).getId();
        UUID b = event.getActivities().get(1).getId();

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            // The stale writer reads the event while both activities are still scheduled.
            events.findWithActivitiesById(event.getId()).orElseThrow();
            var executor = Executors.newSingleThreadExecutor();
            try {
                executor.submit(() -> service.cancelActivity(b, Optional.of(owner))).get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                executor.shutdown();
            }
            service.cancelActivity(a, Optional.of(owner));
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private LifecycleEvent saveAllPrivate() {
        return save(EventLifecycleFixtures.publishedEvent(
                orgId, START, Duration.ofHours(1), ActivityVisibility.PRIVATE, ActivityVisibility.PRIVATE));
    }

    @Test
    @DisplayName("BR-18 the lifecycle view of an all-private event is hidden from guests and strangers")
    void br18_lifecycleViewOfAllPrivateEventIsHidden() {
        LifecycleEvent event = saveAllPrivate();

        assertProblem(() -> service.view(event.getId(), Optional.empty()), HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND");
        assertProblem(() -> service.view(event.getId(), Optional.of(stranger)), HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND");
    }

    @Test
    @DisplayName("BR-18 team members and organization members still see an all-private event")
    void br18_membersSeeAllPrivateEvent() {
        LifecycleEvent event = saveAllPrivate();
        UUID member = UUID.randomUUID();
        when(organizations.isMember(orgId, member)).thenReturn(true);

        assertThat(service.view(event.getId(), Optional.of(editor)).activities()).hasSize(2);
        assertThat(service.view(event.getId(), Optional.of(member)).activities()).hasSize(2);
    }

    @Test
    @DisplayName("BR-18 the cancel endpoints answer not found for strangers on an all-private event")
    void br18_cancelEndpointsAnswerNotFoundForStrangers() {
        LifecycleEvent event = saveAllPrivate();
        UUID a = event.getActivities().get(0).getId();

        assertProblem(() -> service.cancelEvent(event.getId(), Optional.of(stranger)), HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND");
        assertProblem(() -> service.cancelActivity(a, Optional.of(stranger)), HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND");
    }
}
