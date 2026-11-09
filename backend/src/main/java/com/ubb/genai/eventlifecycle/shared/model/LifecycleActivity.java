package com.ubb.genai.eventlifecycle.shared.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Provisional: owned by the event-authoring share (jose03pedro); replaced at reconciliation.
 */
@Entity
@Table(name = "lifecycle_activity")
public class LifecycleActivity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private LifecycleEvent event;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private String description = "";

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(length = 300)
    private String location;

    @Column(name = "online_link", length = 500)
    private String onlineLink;

    private Integer capacity;

    @Column(name = "registration_opens_at")
    private Instant registrationOpensAt;

    @Column(name = "registration_closes_at")
    private Instant registrationClosesAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActivityVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActivityState state = ActivityState.SCHEDULED;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LifecycleAgendaItem> agendaItems = new ArrayList<>();

    protected LifecycleActivity() {
    }

    public static LifecycleActivity scheduled(
            String title, Instant startsAt, Instant endsAt, ActivityVisibility visibility) {
        LifecycleActivity activity = new LifecycleActivity();
        activity.id = UUID.randomUUID();
        activity.title = title;
        activity.startsAt = startsAt;
        activity.endsAt = endsAt;
        activity.visibility = visibility;
        return activity;
    }

    void attachTo(LifecycleEvent event) {
        this.event = event;
    }

    public void cancel(Instant now) {
        if (isCancelled()) {
            throw new IllegalEventTransitionException(
                    "ACTIVITY_ALREADY_CANCELLED", "Activity " + id + " is already cancelled");
        }
        this.state = ActivityState.CANCELLED;
        this.cancelledAt = now;
    }

    public boolean isCancelled() {
        return state == ActivityState.CANCELLED;
    }

    public boolean hasEnded(Instant now) {
        return endsAt != null && !now.isBefore(endsAt);
    }

    public UUID getEventId() {
        return event.getId();
    }

    public UUID getId() {
        return id;
    }

    public LifecycleEvent getEvent() {
        return event;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOnlineLink() {
        return onlineLink;
    }

    public void setOnlineLink(String onlineLink) {
        this.onlineLink = onlineLink;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Instant getRegistrationOpensAt() {
        return registrationOpensAt;
    }

    public Instant getRegistrationClosesAt() {
        return registrationClosesAt;
    }

    public ActivityVisibility getVisibility() {
        return visibility;
    }

    public ActivityState getState() {
        return state;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public List<LifecycleAgendaItem> getAgendaItems() {
        return agendaItems;
    }
}
