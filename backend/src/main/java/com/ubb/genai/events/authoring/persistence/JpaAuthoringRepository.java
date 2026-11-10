package com.ubb.genai.events.authoring.persistence;

import com.ubb.genai.events.authoring.application.AuthoringException;
import com.ubb.genai.events.authoring.domain.AuthoringActivity;
import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAuthoringRepository implements AuthoringEventRepository, com.ubb.genai.events.authoring.ports.PublishedEventsReadContract {
  private final EntityManager em;

  public JpaAuthoringRepository(EntityManager em) {
    this.em = em;
  }

  @org.springframework.transaction.annotation.Transactional(readOnly = true,
      isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
  public List<PublishedEvent> publishedEvents() {
    return em.createQuery("select e from EventEntity e where e.state=:state order by e.id", EventEntity.class)
        .setParameter("state", com.ubb.genai.events.authoring.domain.EventState.PUBLISHED)
        .getResultList().stream().map(this::domain).map(event ->
            new PublishedEvent(event.id(), event.organizationRef(), event.title(), event.description(),
                event.category(), event.state(), event.publishedAt(), event.version(),
                event.activities().stream().map(activity -> new PublishedActivity(activity.id(),
                    activity.title(), activity.description(), activity.visibility(), activity.cancelled(),
                    activity.schedule().start(), activity.schedule().end(), activity.location(),
                    activity.onlineLink(), activity.capacity(),
                    activity.registration().effectiveOpen(event.publishedAt(), activity.createdAt()),
                    activity.registration().effectiveClose(activity.schedule()))).toList())).toList();
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
    int changed = em.createNativeQuery(
            "update event_authoring.event set version=version+1, title=:title, description=:description, category=:category, state=:state, published_at=:published where id=:id and version=:version")
        .setParameter("title", event.title())
        .setParameter("description", event.description())
        .setParameter("category", event.category().name())
        .setParameter("state", event.state().name())
        .setParameter("published", event.publishedAt())
        .setParameter("id", event.id())
        .setParameter("version", event.version())
        .executeUpdate();
    if (changed != 1)
      throw new AuthoringException(409, "VERSION_CONFLICT", "Version conflict");

    for (var row : activities(event.id())) {
      var replacement = event.activities().stream()
          .filter(activity -> activity.id().equals(row.id)).findFirst();
      for (var item : agenda(row.id)) {
        if (replacement.isEmpty()
            || replacement.get().agenda().stream().noneMatch(i -> i.id().equals(item.id)))
          em.remove(item);
      }
      if (replacement.isEmpty()) em.remove(row);
    }
    for (var activity : event.activities()) {
      em.merge(ActivityEntity.from(event.id(), activity));
      for (var item : activity.agenda())
        em.merge(AgendaItemEntity.from(activity.id(), item));
    }
    em.flush();
    em.clear();
    return findById(event.id()).orElseThrow();
  }

  private List<ActivityEntity> activities(UUID eventId) {
    return em.createQuery(
            "select a from ActivityEntity a where a.eventId=:event", ActivityEntity.class)
        .setParameter("event", eventId).getResultList();
  }

  private List<AgendaItemEntity> agenda(UUID activityId) {
    return em.createQuery(
            "select i from AgendaItemEntity i where i.activityId=:activity", AgendaItemEntity.class)
        .setParameter("activity", activityId).getResultList();
  }

  private AuthoringEvent domain(EventEntity row) {
    var children = activities(row.id).stream()
        .map(activity -> activity.toDomain().withAgenda(
            agenda(activity.id).stream().map(AgendaItemEntity::toDomain).toList()))
        .sorted(Comparator.comparing(AuthoringActivity::id)).toList();
    return row.toDomain().withActivities(children);
  }
}
