package com.ubb.genai.eventlifecycle.lifecycle;

import com.ubb.genai.eventlifecycle.shared.access.EventAccessPolicy;
import com.ubb.genai.eventlifecycle.shared.model.ActivityState;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleActivity;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleActivityRepository;
import com.ubb.genai.eventlifecycle.shared.persistence.LifecycleEventRepository;
import com.ubb.genai.eventlifecycle.shared.port.NotificationPort;
import com.ubb.genai.eventlifecycle.shared.port.SignupPort;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cancels activities and events (US-20, US-21) and applies the cascade rules BR-04 to BR-06. */
@Service
public class EventCancellationService {

    private final EventAccessPolicy access;
    private final LifecycleEventRepository events;
    private final LifecycleActivityRepository activities;
    private final SignupPort signups;
    private final NotificationPort notifications;
    private final Clock clock;

    public EventCancellationService(
            EventAccessPolicy access,
            LifecycleEventRepository events,
            LifecycleActivityRepository activities,
            SignupPort signups,
            NotificationPort notifications,
            Clock clock) {
        this.access = access;
        this.events = events;
        this.activities = activities;
        this.signups = signups;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public LifecycleEventView cancelActivity(UUID activityId, Optional<UUID> userId) {
        LifecycleActivity activity = activities.findById(activityId)
                .orElseThrow(() -> LifecycleProblem.notFound("ACTIVITY_NOT_FOUND"));
        LifecycleEvent event = access.requireVisibleEvent(activity.getEventId(), userId);
        access.requireOwnerRights(event, access.requireUser(userId));
        if (event.isReadOnly()) {
            throw LifecycleProblem.conflict("EVENT_READ_ONLY", "The event is " + event.getState() + " and read-only");
        }
        if (activity.isCancelled()) {
            throw LifecycleProblem.conflict("ACTIVITY_ALREADY_CANCELLED", "The activity is already cancelled");
        }
        Instant now = clock.instant();
        if (activity.hasEnded(now)) {
            throw LifecycleProblem.conflict("ACTIVITY_ENDED", "The activity has already ended");
        }

        activity.cancel(now);
        Set<UUID> recipients = signups.cancelAllSignups(activityId);
        boolean eventCancelled = event.allActivitiesCancelled();
        if (eventCancelled) {
            event.cancel(now);
        }
        events.save(event);

        notifications.activityCancelled(activityId, recipients);
        if (eventCancelled) {
            notifications.eventCancelled(event.getId(), recipients);
        }
        return toView(event, userId);
    }

    @Transactional
    public LifecycleEventView cancelEvent(UUID eventId, Optional<UUID> userId) {
        LifecycleEvent event = access.requireVisibleEvent(eventId, userId);
        access.requireOwnerRights(event, access.requireUser(userId));
        if (event.isReadOnly()) {
            throw LifecycleProblem.conflict(
                    "EVENT_NOT_CANCELLABLE", "The event is " + event.getState() + " and cannot be cancelled");
        }
        Instant now = clock.instant();

        List<UUID> scheduled = new ArrayList<>();
        for (LifecycleActivity activity : event.getActivities()) {
            if (activity.getState() == ActivityState.SCHEDULED) {
                scheduled.add(activity.getId());
            }
        }
        Set<UUID> recipients = new HashSet<>();
        scheduled.forEach(id -> recipients.addAll(signups.cancelAllSignups(id)));
        event.cancel(now);
        events.save(event);

        notifications.eventCancelled(eventId, recipients);
        return toView(event, userId);
    }

    @Transactional(readOnly = true)
    public LifecycleEventView view(UUID eventId, Optional<UUID> userId) {
        return toView(access.requireVisibleEvent(eventId, userId), userId);
    }

    private LifecycleEventView toView(LifecycleEvent event, Optional<UUID> userId) {
        boolean owner = userId.map(id -> access.hasOwnerRights(event, id)).orElse(false);
        boolean team = owner || userId.map(id -> access.isTeamMember(event, id)).orElse(false);
        List<LifecycleEventView.ActivityView> activityViews = event.getActivities().stream()
                .filter(a -> team || access.canSeeActivity(a, event.getOrganizationId(), userId))
                .map(a -> new LifecycleEventView.ActivityView(
                        a.getId(), a.getTitle(), a.getStartsAt(), a.getEndsAt(), a.getState()))
                .toList();
        return new LifecycleEventView(
                event.getId(),
                event.getTitle(),
                event.getState(),
                activityViews,
                new LifecycleEventView.Permissions(
                        owner && !event.isReadOnly(), owner && event.getState() == EventState.DRAFT));
    }
}
