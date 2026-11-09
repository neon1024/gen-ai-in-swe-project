package com.ubb.genai.eventlifecycle.shared.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provisional: owned by the event-authoring share (jose03pedro); replaced at reconciliation.
 */
@Entity
@Table(name = "lifecycle_event")
public class LifecycleEvent {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventState state = EventState.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LifecycleActivity> activities = new ArrayList<>();

    protected LifecycleEvent() {
    }

    public static LifecycleEvent draft(
            UUID organizationId, String title, String description, EventCategory category) {
        LifecycleEvent event = new LifecycleEvent();
        event.id = UUID.randomUUID();
        event.organizationId = organizationId;
        event.title = title;
        event.description = description;
        event.category = category;
        return event;
    }

    public void addActivity(LifecycleActivity activity) {
        activity.attachTo(this);
        activities.add(activity);
    }

    public void publish(Instant now) {
        if (state != EventState.DRAFT) {
            throw new IllegalEventTransitionException(
                    "EVENT_NOT_PUBLISHABLE", "Event " + id + " is " + state + " and cannot be published");
        }
        this.state = EventState.PUBLISHED;
        this.publishedAt = now;
    }

    public void cancel(Instant now) {
        if (state != EventState.DRAFT && state != EventState.PUBLISHED) {
            throw new IllegalEventTransitionException(
                    "EVENT_NOT_CANCELLABLE", "Event " + id + " is " + state + " and cannot be cancelled");
        }
        activities.stream()
                .filter(a -> a.getState() == ActivityState.SCHEDULED)
                .forEach(a -> a.cancel(now));
        this.state = EventState.CANCELLED;
        this.cancelledAt = now;
    }

    public void complete(Instant now) {
        if (state != EventState.PUBLISHED) {
            throw new IllegalEventTransitionException(
                    "EVENT_NOT_COMPLETABLE", "Event " + id + " is " + state + " and cannot be completed");
        }
        this.state = EventState.COMPLETED;
        this.completedAt = now;
    }

    public boolean isReadOnly() {
        return state == EventState.COMPLETED || state == EventState.CANCELLED;
    }

    public boolean allActivitiesCancelled() {
        return !activities.isEmpty() && activities.stream().allMatch(LifecycleActivity::isCancelled);
    }

    public Optional<Instant> lastScheduledActivityEnd() {
        return activities.stream()
                .filter(a -> !a.isCancelled())
                .map(LifecycleActivity::getEndsAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder());
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public EventState getState() {
        return state;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<LifecycleActivity> getActivities() {
        return activities;
    }
}
