package com.ubb.genai.events.authoring.persistence;

import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import com.ubb.genai.events.authoring.domain.EventCategory;
import com.ubb.genai.events.authoring.domain.EventState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event", schema = "event_authoring")
public class EventEntity {
  @Id UUID id;

  @Column(name = "organization_ref", nullable = false, columnDefinition = "text")
  String organizationRef;

  @Column(name = "owner_subject", nullable = false, columnDefinition = "text")
  String ownerSubject;

  @Column(nullable = false, columnDefinition = "text")
  String title;

  @Column(nullable = false, columnDefinition = "text")
  String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "text")
  EventCategory category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "text")
  EventState state;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  @Column(name = "published_at")
  Instant publishedAt;

  @Version long version;

  protected EventEntity() {}

  static EventEntity from(AuthoringEvent e) {
    var row = new EventEntity();
    row.id = e.id();
    row.organizationRef = e.organizationRef();
    row.ownerSubject = e.ownerSubject();
    row.title = e.title();
    row.description = e.description();
    row.category = e.category();
    row.state = e.state();
    row.createdAt = e.createdAt();
    row.publishedAt = e.publishedAt();
    row.version = e.version();
    return row;
  }

  AuthoringEvent toDomain() {
    return new AuthoringEvent(
        id,
        organizationRef,
        ownerSubject,
        title,
        description,
        category,
        state,
        createdAt,
        publishedAt,
        version);
  }
}
