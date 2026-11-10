package com.ubb.genai.eventlifecycle.lifecycle;

import com.ubb.genai.eventlifecycle.shared.model.ActivityState;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Event state returned by the cancellation endpoints. */
public record LifecycleEventView(
        UUID id, String title, EventState state, List<ActivityView> activities, Permissions permissions) {

    public record ActivityView(UUID id, String title, Instant startsAt, Instant endsAt, ActivityState state) {
    }

    public record Permissions(boolean canCancel, boolean canDelete) {
    }
}
