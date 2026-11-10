package com.ubb.genai.eventlifecycle.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ubb.genai.eventlifecycle.EventLifecycleFixtures;
import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class EventReadOnlyGuardTest {

    private static final Instant START = Instant.parse("2027-02-01T10:00:00Z");

    private final LifecycleEventRepository events = mock(LifecycleEventRepository.class);
    private final EventReadOnlyGuard guard = new EventReadOnlyGuard(events);
    private final UUID id = UUID.randomUUID();

    private LifecycleEvent published() {
        return EventLifecycleFixtures.publishedEvent(UUID.randomUUID(), START, Duration.ofHours(1), ActivityVisibility.PUBLIC);
    }

    @Test
    @DisplayName("BR-06 the guard refuses completed and cancelled events")
    void br06_readOnlyGuardRefusesCompletedAndCancelled() {
        LifecycleEvent completed = published();
        completed.complete(START);
        LifecycleEvent cancelled = published();
        cancelled.cancel(START);

        for (LifecycleEvent event : new LifecycleEvent[] {completed, cancelled}) {
            when(events.findById(id)).thenReturn(Optional.of(event));
            assertThatThrownBy(() -> guard.requireEditable(id))
                    .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                        assertThat(p.status()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(p.code()).isEqualTo("EVENT_READ_ONLY");
                    });
        }
    }

    @Test
    @DisplayName("BR-06 the guard allows draft and published events")
    void br06_readOnlyGuardAllowsDraftAndPublished() {
        when(events.findById(id)).thenReturn(Optional.of(EventLifecycleFixtures.draftEvent(UUID.randomUUID())));
        assertThatCode(() -> guard.requireEditable(id)).doesNotThrowAnyException();

        when(events.findById(id)).thenReturn(Optional.of(published()));
        assertThatCode(() -> guard.requireEditable(id)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("BR-06 the guard reports a missing event as not found")
    void br06_readOnlyGuardReportsMissingEvent() {
        when(events.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireEditable(id))
                .isInstanceOfSatisfying(LifecycleProblem.class, p -> {
                    assertThat(p.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(p.code()).isEqualTo("EVENT_NOT_FOUND");
                });
    }
}
