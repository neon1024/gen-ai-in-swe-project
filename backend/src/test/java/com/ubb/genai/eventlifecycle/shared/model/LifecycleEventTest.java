package com.ubb.genai.eventlifecycle.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LifecycleEventTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");

    private static LifecycleEvent draft() {
        return LifecycleEvent.draft(UUID.randomUUID(), "Title", "Description", EventCategory.WORKSHOP);
    }

    private static LifecycleActivity activity(String start, String end) {
        return LifecycleActivity.scheduled(
                "A", Instant.parse(start), Instant.parse(end), ActivityVisibility.PUBLIC);
    }

    @Test
    @DisplayName("BR-01 a draft can be published")
    void br01_draftCanBePublished() {
        LifecycleEvent event = draft();
        event.publish(NOW);
        assertThat(event.getState()).isEqualTo(EventState.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("BR-01 draft and published events can be cancelled with their activities")
    void br01_draftAndPublishedCanBeCancelled() {
        LifecycleEvent published = draft();
        published.addActivity(activity("2026-12-01T09:00:00Z", "2026-12-01T10:00:00Z"));
        published.addActivity(activity("2026-12-02T09:00:00Z", "2026-12-02T10:00:00Z"));
        published.publish(NOW);
        LifecycleEvent draft = draft();
        draft.addActivity(activity("2026-12-01T09:00:00Z", "2026-12-01T10:00:00Z"));

        for (LifecycleEvent event : new LifecycleEvent[] {published, draft}) {
            event.cancel(NOW);
            assertThat(event.getState()).isEqualTo(EventState.CANCELLED);
            assertThat(event.getCancelledAt()).isEqualTo(NOW);
            assertThat(event.getActivities()).isNotEmpty().allSatisfy(a -> {
                assertThat(a.getState()).isEqualTo(ActivityState.CANCELLED);
                assertThat(a.getCancelledAt()).isEqualTo(NOW);
            });
        }
    }

    @Test
    @DisplayName("BR-01 a completed or cancelled event cannot be cancelled")
    void br01_completedOrCancelledEventCannotBeCancelled() {
        LifecycleEvent cancelled = draft();
        cancelled.cancel(NOW);
        LifecycleEvent completed = draft();
        completed.publish(NOW);
        completed.complete(NOW);

        for (LifecycleEvent event : new LifecycleEvent[] {cancelled, completed}) {
            assertThatThrownBy(() -> event.cancel(NOW))
                    .isInstanceOf(IllegalEventTransitionException.class)
                    .extracting(e -> ((IllegalEventTransitionException) e).code())
                    .isEqualTo("EVENT_NOT_CANCELLABLE");
        }
    }

    @Test
    @DisplayName("BR-01 only a published event can complete")
    void br01_onlyPublishedEventCanComplete() {
        LifecycleEvent draft = draft();
        assertThatThrownBy(() -> draft.complete(NOW))
                .isInstanceOf(IllegalEventTransitionException.class)
                .extracting(e -> ((IllegalEventTransitionException) e).code())
                .isEqualTo("EVENT_NOT_COMPLETABLE");

        LifecycleEvent published = draft();
        published.publish(NOW);
        published.complete(NOW);
        assertThat(published.getState()).isEqualTo(EventState.COMPLETED);
        assertThat(published.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("BR-06 completed and cancelled events are read-only")
    void br06_completedAndCancelledEventsAreReadOnly() {
        LifecycleEvent event = draft();
        assertThat(event.isReadOnly()).isFalse();
        event.publish(NOW);
        assertThat(event.isReadOnly()).isFalse();
        event.complete(NOW);
        assertThat(event.isReadOnly()).isTrue();

        LifecycleEvent cancelled = draft();
        cancelled.cancel(NOW);
        assertThat(cancelled.isReadOnly()).isTrue();
    }

    @Test
    @DisplayName("BR-04 the last scheduled activity end ignores cancelled activities")
    void br04_lastScheduledActivityEndIgnoresCancelledActivities() {
        LifecycleEvent event = draft();
        LifecycleActivity late = activity("2026-12-01T09:00:00Z", "2026-12-01T10:00:00Z");
        event.addActivity(late);
        event.addActivity(activity("2026-12-01T08:00:00Z", "2026-12-01T09:00:00Z"));
        late.cancel(NOW);

        assertThat(event.lastScheduledActivityEnd()).isEqualTo(Optional.of(Instant.parse("2026-12-01T09:00:00Z")));
    }
}
