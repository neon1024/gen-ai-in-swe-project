package com.ubb.genai.events.authoring.persistence;

import com.ubb.genai.events.authoring.domain.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "activity", schema = "event_authoring")
public class ActivityEntity {
  @Id UUID id;
  @Column(name = "event_id", nullable = false) UUID eventId;
  @Column(nullable = false, columnDefinition = "text") String title;
  @Column(nullable = false, columnDefinition = "text") String description;
  @Column(name = "starts_at", nullable = false) Instant start;
  @Column(name = "ends_at", nullable = false) Instant end;
  @Column(name = "start_offset_seconds", nullable = false) int startOffset;
  @Column(name = "end_offset_seconds", nullable = false) int endOffset;
  @Column(columnDefinition = "text") String location;
  @Column(name = "online_link", columnDefinition = "text") String onlineLink;
  @Column(nullable = false) int capacity;
  @Column(name = "registration_open") Instant registrationOpen;
  @Column(name = "registration_close") Instant registrationClose;
  @Column(name = "registration_open_offset_seconds") Integer registrationOpenOffset;
  @Column(name = "registration_close_offset_seconds") Integer registrationCloseOffset;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "text") ActivityVisibility visibility;
  @Column(nullable = false) boolean cancelled;
  @Column(name = "created_at", nullable = false) Instant createdAt;

  protected ActivityEntity() {}

  static ActivityEntity from(UUID eventId, AuthoringActivity activity) {
    var row = new ActivityEntity();
    row.id = activity.id();
    row.eventId = eventId;
    row.title = activity.title();
    row.description = activity.description();
    row.start = activity.schedule().start().toInstant();
    row.end = activity.schedule().end().toInstant();
    row.startOffset = activity.schedule().start().getOffset().getTotalSeconds();
    row.endOffset = activity.schedule().end().getOffset().getTotalSeconds();
    row.location = activity.location();
    row.onlineLink = activity.onlineLink();
    row.capacity = activity.capacity();
    row.registrationOpen = instant(activity.registration().open());
    row.registrationClose = instant(activity.registration().close());
    row.registrationOpenOffset = offset(activity.registration().open());
    row.registrationCloseOffset = offset(activity.registration().close());
    row.visibility = activity.visibility();
    row.cancelled = activity.cancelled();
    row.createdAt = activity.createdAt();
    return row;
  }

  AuthoringActivity toDomain() {
    return new AuthoringActivity(id, title, description,
        new ActivitySchedule(date(start, startOffset), date(end, endOffset)), location, onlineLink,
        capacity, new RegistrationWindow(date(registrationOpen, registrationOpenOffset),
        date(registrationClose, registrationCloseOffset)), visibility, cancelled, createdAt);
  }

  private static Instant instant(OffsetDateTime value) { return value == null ? null : value.toInstant(); }
  private static Integer offset(OffsetDateTime value) { return value == null ? null : value.getOffset().getTotalSeconds(); }
  static OffsetDateTime date(Instant instant, Integer offset) {
    return instant == null ? null : instant.atOffset(ZoneOffset.ofTotalSeconds(offset));
  }
}
