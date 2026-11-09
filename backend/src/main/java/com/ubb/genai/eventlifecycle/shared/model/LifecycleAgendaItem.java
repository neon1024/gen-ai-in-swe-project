package com.ubb.genai.eventlifecycle.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Provisional: owned by the event-authoring share (jose03pedro); replaced at reconciliation.
 */
@Entity
@Table(name = "lifecycle_agenda_item")
public class LifecycleAgendaItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private LifecycleActivity activity;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String speaker;

    protected LifecycleAgendaItem() {
    }

    public LifecycleAgendaItem(LifecycleActivity activity, Instant startsAt, String title, String speaker) {
        this.id = UUID.randomUUID();
        this.activity = activity;
        this.startsAt = startsAt;
        this.title = title;
        this.speaker = speaker;
        activity.getAgendaItems().add(this);
    }

    public UUID getId() {
        return id;
    }

    public LifecycleActivity getActivity() {
        return activity;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public String getTitle() {
        return title;
    }

    public String getSpeaker() {
        return speaker;
    }
}
