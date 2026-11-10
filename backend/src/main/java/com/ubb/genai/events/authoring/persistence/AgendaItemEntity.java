package com.ubb.genai.events.authoring.persistence;

import com.ubb.genai.events.authoring.domain.AgendaItem;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agenda_item", schema = "event_authoring")
public class AgendaItemEntity {
  @Id UUID id;
  @Column(name = "activity_id", nullable = false) UUID activityId;
  @Column(nullable = false) Instant time;
  @Column(name = "offset_seconds", nullable = false) int offset;
  @Column(nullable = false, columnDefinition = "text") String title;
  @Column(columnDefinition = "text") String speaker;

  protected AgendaItemEntity() {}

  static AgendaItemEntity from(UUID activityId, AgendaItem item) {
    var row = new AgendaItemEntity();
    row.id = item.id();
    row.activityId = activityId;
    row.time = item.time().toInstant();
    row.offset = item.time().getOffset().getTotalSeconds();
    row.title = item.title();
    row.speaker = item.speaker();
    return row;
  }

  AgendaItem toDomain() {
    return new AgendaItem(id, ActivityEntity.date(time, offset), title, speaker);
  }
}
