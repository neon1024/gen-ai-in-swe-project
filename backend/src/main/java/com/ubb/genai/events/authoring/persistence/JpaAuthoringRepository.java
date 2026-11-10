package com.ubb.genai.events.authoring.persistence;

import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAuthoringRepository implements AuthoringEventRepository {
  private final EntityManager em;

  public JpaAuthoringRepository(EntityManager em) {
    this.em = em;
  }

  public AuthoringEvent save(AuthoringEvent event) {
    var row = EventEntity.from(event);
    em.persist(row);
    em.flush();
    return row.toDomain();
  }

  public Optional<AuthoringEvent> findById(UUID id) {
    return Optional.ofNullable(em.find(EventEntity.class, id)).map(this::domain);
  }
  public Optional<AuthoringEvent> findForUpdate(UUID id) {
    return Optional.ofNullable(em.find(EventEntity.class, id, LockModeType.PESSIMISTIC_WRITE))
        .map(this::domain);
  }

  public AuthoringEvent replace(AuthoringEvent event) {
    // The service holds the Event lock. Explicitly bump the root even for child-only writes.
    int changed = em.createNativeQuery("update event_authoring.event set version=version+1 where id=:id and version=:version")
        .setParameter("id", event.id()).setParameter("version", event.version()).executeUpdate();
    if (changed != 1) throw new com.ubb.genai.events.authoring.application.AuthoringException(409, "VERSION_CONFLICT", "Version conflict");
    var current = activities(event.id());
    for (var row : current) {
      if (event.activities().stream().noneMatch(activity -> activity.id().equals(row.id))) em.remove(row);
    }
    for (var activity : event.activities()) em.merge(ActivityEntity.from(event.id(), activity));
    em.flush();
    em.clear();
    return findById(event.id()).orElseThrow();
  }

  private java.util.List<ActivityEntity> activities(UUID eventId) {
    return em.createQuery("select a from ActivityEntity a where a.eventId=:event", ActivityEntity.class)
        .setParameter("event", eventId).getResultList();
  }

  private AuthoringEvent domain(EventEntity row) {
    var children = activities(row.id).stream().map(ActivityEntity::toDomain)
        .sorted(Comparator.comparing(com.ubb.genai.events.authoring.domain.AuthoringActivity::id)).toList();
    return row.toDomain().withActivities(children);
  }
}
