package com.ubb.genai.eventlifecycle;

import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.EventCategory;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class EventLifecycleFixtures {

    private EventLifecycleFixtures() {
    }

    /** Builds a published event with one activity per visibility, each starting one hour after the previous. */
    public static LifecycleEvent publishedEvent(
            UUID orgId, Instant start, Duration length, ActivityVisibility... activities) {
        LifecycleEvent event = draftEvent(orgId);
        Instant activityStart = start;
        for (int i = 0; i < activities.length; i++) {
            event.addActivity(LifecycleActivity.scheduled(
                    "Activity " + (i + 1), activityStart, activityStart.plus(length), activities[i]));
            activityStart = activityStart.plus(Duration.ofHours(1));
        }
        event.publish(start.minus(Duration.ofDays(1)));
        return event;
    }

    public static LifecycleEvent draftEvent(UUID orgId) {
        return LifecycleEvent.draft(orgId, "Sample event", "Sample description", EventCategory.WORKSHOP);
    }
}
