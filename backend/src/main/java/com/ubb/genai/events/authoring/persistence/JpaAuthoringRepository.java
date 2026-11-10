package com.ubb.genai.events.authoring.persistence;

import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import jakarta.persistence.EntityManager;
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
    return Optional.ofNullable(em.find(EventEntity.class, id)).map(EventEntity::toDomain);
  }
}
