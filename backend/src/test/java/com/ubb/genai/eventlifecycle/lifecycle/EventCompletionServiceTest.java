package com.ubb.genai.eventlifecycle.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubb.genai.eventlifecycle.EventLifecycleFixtures;
import com.ubb.genai.eventlifecycle.MutableClock;
import com.ubb.genai.eventlifecycle.TestClockConfiguration;
import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

/**
 * The shared test database keeps rows from other test classes, so every event here lives in 2020, long before
 * the dates other classes use (2027), and the clock is set inside 2020. Only this class's fixtures can qualify,
 * which makes the returned counts exact. Created events are deleted after each test.
 */
@SpringBootTest
@Import(TestClockConfiguration.class)
class EventCompletionServiceTest {

    private static final Instant START = Instant.parse("2020-03-01T10:00:00Z");

    @Autowired
    private EventCompletionService service;

    @Autowired
    private LifecycleEventRepository events;

    @Autowired
    private EventReadOnlyGuard guard;

    @Autowired
    private MutableClock clock;

    private final UUID orgId = UUID.randomUUID();
    private final List<UUID> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clock.setInstant(START);
    }

    @AfterEach
    void cleanUp() {
        events.deleteAllById(created);
    }

    private LifecycleEvent savePublished(Duration... lengths) {
        LifecycleEvent event = EventLifecycleFixtures.draftEvent(orgId);
        for (int i = 0; i < lengths.length; i++) {
            event.addActivity(LifecycleActivity.scheduled(
                    "Activity " + (i + 1), START, START.plus(lengths[i]), ActivityVisibility.PUBLIC));
        }
        event.publish(START.minus(Duration.ofDays(1)));
        return save(event);
    }

    private LifecycleEvent save(LifecycleEvent event) {
        LifecycleEvent saved = events.saveAndFlush(event);
        created.add(saved.getId());
        return saved;
    }

    private LifecycleEvent reload(UUID id) {
        return events.findWithActivitiesById(id).orElseThrow();
    }

    @Test
    @DisplayName("US-23 an event completes when its last scheduled activity ends")
    void us23_eventCompletesWhenLastScheduledActivityEnds() {
        LifecycleEvent event = savePublished(Duration.ofHours(1), Duration.ofHours(2));
        clock.setInstant(START.plus(Duration.ofHours(2)));

        assertThat(service.completeEndedEvents()).isEqualTo(1);

        LifecycleEvent reloaded = reload(event.getId());
        assertThat(reloaded.getState()).isEqualTo(EventState.COMPLETED);
        assertThat(reloaded.getCompletedAt()).isEqualTo(START.plus(Duration.ofHours(2)));
    }

    @Test
    @DisplayName("US-23 an event with a future activity stays published")
    void us23_eventWithFutureActivityStaysPublished() {
        LifecycleEvent event = savePublished(Duration.ofHours(1), Duration.ofHours(3));
        clock.setInstant(START.plus(Duration.ofHours(2)));

        assertThat(service.completeEndedEvents()).isZero();

        assertThat(reload(event.getId()).getState()).isEqualTo(EventState.PUBLISHED);
    }

    @Test
    @DisplayName("BR-04 a cancelled later activity does not delay completion")
    void br04_cancelledLaterActivityDoesNotDelayCompletion() {
        LifecycleEvent event = savePublished(Duration.ofHours(1), Duration.ofHours(3));
        LifecycleEvent loaded = reload(event.getId());
        loaded.getActivities().get(1).cancel(START);
        events.saveAndFlush(loaded);
        clock.setInstant(START.plus(Duration.ofMinutes(90)));

        assertThat(service.completeEndedEvents()).isEqualTo(1);

        assertThat(reload(event.getId()).getState()).isEqualTo(EventState.COMPLETED);
    }

    @Test
    @DisplayName("BR-01 draft and cancelled events are never completed")
    void br01_draftAndCancelledEventsAreNeverCompleted() {
        LifecycleEvent draft = EventLifecycleFixtures.draftEvent(orgId);
        draft.addActivity(LifecycleActivity.scheduled(
                "Past", START, START.plus(Duration.ofHours(1)), ActivityVisibility.PUBLIC));
        draft = save(draft);
        LifecycleEvent cancelled = savePublished(Duration.ofHours(1));
        LifecycleEvent toCancel = reload(cancelled.getId());
        toCancel.cancel(START);
        events.saveAndFlush(toCancel);
        clock.setInstant(START.plus(Duration.ofDays(1)));

        assertThat(service.completeEndedEvents()).isZero();

        assertThat(reload(draft.getId()).getState()).isEqualTo(EventState.DRAFT);
        assertThat(reload(cancelled.getId()).getState()).isEqualTo(EventState.CANCELLED);
    }

    @Test
    @DisplayName("US-23 running the job twice is idempotent")
    void us23_runningTwiceIsIdempotent() {
        LifecycleEvent event = savePublished(Duration.ofHours(1));
        clock.setInstant(START.plus(Duration.ofHours(1)));

        assertThat(service.completeEndedEvents()).isEqualTo(1);
        assertThat(service.completeEndedEvents()).isZero();

        assertThat(reload(event.getId()).getCompletedAt()).isEqualTo(START.plus(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("US-23 a completed event is read-only")
    void us23_completedEventIsReadOnly() {
        LifecycleEvent event = savePublished(Duration.ofHours(1));
        clock.setInstant(START.plus(Duration.ofHours(1)));
        service.completeEndedEvents();

        assertThatThrownBy(() -> guard.requireEditable(event.getId()))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(p.code()).isEqualTo("EVENT_READ_ONLY");
                });
    }
}
