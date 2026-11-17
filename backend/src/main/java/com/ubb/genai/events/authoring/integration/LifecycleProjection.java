package com.ubb.genai.events.authoring.integration;

import com.ubb.genai.events.authoring.api.AuthoringViews.ActivityView;
import com.ubb.genai.events.authoring.api.AuthoringViews.EventView;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interim reconciliation of authoring (K-7) with the lifecycle share: a Published authoring event is
 * mirrored, under the same ids, into the lifecycle tables so search, cancellation, feedback and
 * reports see it. Authoring stays the source of truth for content; lifecycle stays the source of
 * truth for state, so an event the lifecycle share has cancelled or completed is never touched.
 * Remove once lifecycle reads the authoring model directly.
 */
@Component
public class LifecycleProjection {
  private final JdbcTemplate jdbc;

  public LifecycleProjection(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Organization refs are opaque strings; lifecycle needs a UUID. */
  static UUID organizationId(String ref) {
    try {
      return UUID.fromString(ref);
    } catch (IllegalArgumentException e) {
      return UUID.nameUUIDFromBytes(ref.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Transactional
  public void sync(EventView e) {
    if (!"Published".equals(e.state())) return;
    List<String> lifecycleState =
        jdbc.queryForList("select state from lifecycle_event where id = ?", String.class, e.id());
    if (!lifecycleState.isEmpty() && !lifecycleState.get(0).equals("PUBLISHED")) return;

    jdbc.update(
        """
        insert into lifecycle_event (id, organization_id, title, description, category, state, published_at)
        values (?, ?, ?, ?, ?, 'PUBLISHED', ?)
        on conflict (id) do update set title = excluded.title, description = excluded.description,
          category = excluded.category, version = lifecycle_event.version + 1
        """,
        e.id(), organizationId(e.organizationRef()), e.title(), e.description(),
        e.category().toUpperCase(Locale.ROOT), Timestamp.from(e.publishedAt()));

    for (ActivityView a : e.activities()) {
      jdbc.update(
          """
          insert into lifecycle_activity (id, event_id, title, description, starts_at, ends_at, location,
            online_link, capacity, registration_opens_at, registration_closes_at, visibility)
          values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          on conflict (id) do update set title = excluded.title, description = excluded.description,
            starts_at = excluded.starts_at, ends_at = excluded.ends_at, location = excluded.location,
            online_link = excluded.online_link, capacity = excluded.capacity,
            registration_opens_at = excluded.registration_opens_at,
            registration_closes_at = excluded.registration_closes_at, visibility = excluded.visibility
          """,
          a.id(), e.id(), a.title(), a.description(), a.start(), a.end(), a.location(), a.onlineLink(),
          a.capacity(), ts(a.effectiveRegistrationOpen()), ts(a.effectiveRegistrationClose()),
          a.visibility().toUpperCase(Locale.ROOT));
      jdbc.update("delete from lifecycle_agenda_item where activity_id = ?", a.id());
      a.agenda().forEach(i -> jdbc.update(
          "insert into lifecycle_agenda_item (id, activity_id, starts_at, title, speaker) values (?, ?, ?, ?, ?)",
          i.id(), a.id(), i.time(), i.title(), i.speaker()));
    }
    // Authoring forbids removing an activity that has signups, so a missing one is safe to drop.
    Object[] keep = e.activities().stream().map(ActivityView::id).toArray();
    jdbc.update(
        "delete from lifecycle_activity where event_id = ? and not (id = any (?::uuid[]))",
        e.id(), toArray(keep));
  }

  private static Timestamp ts(Instant i) {
    return i == null ? null : Timestamp.from(i);
  }

  private static String toArray(Object[] ids) {
    StringBuilder sb = new StringBuilder("{");
    for (int i = 0; i < ids.length; i++) sb.append(i == 0 ? "" : ",").append(ids[i]);
    return sb.append('}').toString();
  }
}
