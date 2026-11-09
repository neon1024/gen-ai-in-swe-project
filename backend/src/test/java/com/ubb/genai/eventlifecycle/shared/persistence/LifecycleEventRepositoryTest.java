package com.ubb.genai.eventlifecycle.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubb.genai.eventlifecycle.EventLifecycleFixtures;
import com.ubb.genai.eventlifecycle.shared.model.ActivityVisibility;
import com.ubb.genai.eventlifecycle.shared.model.EventState;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleAgendaItem;
import com.ubb.genai.eventlifecycle.shared.model.LifecycleEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LifecycleEventRepositoryTest {

    @Autowired
    private LifecycleEventRepository events;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("BR-01 an event with activities and agenda round-trips through the migration")
    void br01_eventWithActivitiesAndAgendaRoundTrips() {
        LifecycleEvent event = EventLifecycleFixtures.publishedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-12-01T09:00:00Z"),
                Duration.ofHours(1),
                ActivityVisibility.PUBLIC,
                ActivityVisibility.PRIVATE);
        new LifecycleAgendaItem(
                event.getActivities().get(0), Instant.parse("2026-12-01T09:00:00Z"), "Opening", "Ada");

        LifecycleEvent saved = events.saveAndFlush(event);
        entityManager.clear();

        LifecycleEvent reloaded = events.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getState()).isEqualTo(EventState.PUBLISHED);
        assertThat(reloaded.getCategory()).isEqualTo(event.getCategory());
        assertThat(reloaded.getActivities()).hasSize(2);
        assertThat(reloaded.getActivities())
                .flatExtracting(a -> a.getAgendaItems())
                .extracting(LifecycleAgendaItem::getTitle)
                .containsExactly("Opening");
    }

    @Test
    @DisplayName("BR-01 the database rejects an unknown event state")
    void br01_databaseRejectsUnknownState() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into lifecycle_event (id, organization_id, title, description, category, state)"
                        + " values (?, ?, 't', 'd', 'WORKSHOP', 'ARCHIVED')",
                UUID.randomUUID(),
                UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
