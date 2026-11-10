package com.ubb.genai.eventlifecycle.lifecycle;

import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Completes published events once their last scheduled activity has ended (US-23, BR-01, BR-04, BR-06). */
@Service
public class EventCompletionService {

    private final LifecycleEventRepository events;
    private final Clock clock;

    public EventCompletionService(LifecycleEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    /**
     * Completes every PUBLISHED event whose last non-cancelled activity ended at or before now. Events without a
     * scheduled activity are skipped. The whole run is one transaction: if a concurrent cancellation makes a save
     * fail, the run rolls back and the next scheduled run retries.
     *
     * @return how many events were completed
     */
    @Transactional
    public int completeEndedEvents() {
        Instant now = clock.instant();
        int completed = 0;
        for (LifecycleEvent event : events.findPublishedWithActivities()) {
            if (event.lastScheduledActivityEnd().filter(end -> !end.isAfter(now)).isPresent()) {
                event.complete(now);
                completed++;
            }
        }
        return completed;
    }
}
