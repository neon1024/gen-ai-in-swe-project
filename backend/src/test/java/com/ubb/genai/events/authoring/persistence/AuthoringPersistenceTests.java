package com.ubb.genai.events.authoring.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import com.ubb.genai.events.authoring.domain.EventCategory;
import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class AuthoringPersistenceTests {
  @Autowired EntityManagerFactory factory;

  @Test
  void committedDraftRoundTripsAndFailedTransactionLeavesNoRow() {
    var draft =
        AuthoringEvent.draft(
            "opaque-org",
            "actual-owner",
            "Title",
            "Description",
            EventCategory.OTHER,
            Instant.parse("2026-11-10T10:00:00Z"));
    try (var em = factory.createEntityManager()) {
      em.getTransaction().begin();
      var saved = new JpaAuthoringRepository(em).save(draft);
      assertEquals(0, saved.version());
      em.getTransaction().commit();
    }
    try (var em = factory.createEntityManager()) {
      assertEquals(draft, new JpaAuthoringRepository(em).findById(draft.id()).orElseThrow());
      var rolledBack =
          AuthoringEvent.draft("org", "owner", "T", "D", EventCategory.OTHER, Instant.now());
      em.getTransaction().begin();
      new JpaAuthoringRepository(em).save(rolledBack);
      em.getTransaction().rollback();
      em.clear();
      assertTrue(new JpaAuthoringRepository(em).findById(rolledBack.id()).isEmpty());
    }
  }

  @Test
  void staleJpaVersionCannotOverwriteCommittedChange() {
    var draft =
        AuthoringEvent.draft(
            "org", "owner", "Original", "Description", EventCategory.OTHER, Instant.now());
    try (var em = factory.createEntityManager()) {
      em.getTransaction().begin();
      new JpaAuthoringRepository(em).save(draft);
      em.getTransaction().commit();
    }
    try (var stale = factory.createEntityManager();
        var current = factory.createEntityManager()) {
      stale.getTransaction().begin();
      var staleRow = stale.find(EventEntity.class, draft.id());
      current.getTransaction().begin();
      var currentRow = current.find(EventEntity.class, draft.id());
      currentRow.title = "Committed";
      current.getTransaction().commit();
      assertEquals(1, currentRow.version);
      staleRow.title = "Stale overwrite";
      assertThrows(OptimisticLockException.class, stale::flush);
      stale.getTransaction().rollback();
    }
    try (var em = factory.createEntityManager()) {
      var stored = new JpaAuthoringRepository(em).findById(draft.id()).orElseThrow();
      assertEquals("Committed", stored.title());
      assertEquals(1, stored.version());
    }
  }
}
