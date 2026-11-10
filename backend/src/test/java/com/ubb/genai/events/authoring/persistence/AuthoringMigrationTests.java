package com.ubb.genai.events.authoring.persistence;

import com.ubb.genai.events.authoring.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class AuthoringMigrationTests {
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("NFR-002: Flyway creates the three authoring tables")
    void createsTheThreeAuthoringTables() {
        assertThat(jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = 'event_authoring' and table_name <> 'flyway_schema_history' order by table_name
                """, String.class)).containsExactly("activity", "agenda_item", "event");
    }

    @Test
    @DisplayName("NFR-002: authoring parent references have foreign keys and indexes")
    void createsParentForeignKeysAndIndexes() {
        assertThat(jdbc.queryForList("""
                select table_name from information_schema.table_constraints
                where constraint_schema = 'event_authoring' and constraint_type = 'FOREIGN KEY'
                order by table_name
                """, String.class)).containsExactly("activity", "agenda_item");
        assertThat(jdbc.queryForList("""
                select indexdef from pg_indexes where schemaname = 'event_authoring'
                """, String.class)).anyMatch(index -> index.contains("(event_id)"))
                .anyMatch(index -> index.contains("(activity_id)"));
    }

    @Test
    @DisplayName("US-14/US-15: opaque external references and original schedule offsets round-trip")
    @Transactional
    void acceptsOpaqueExternalReferencesAndPreservesOffsetMetadata() {
        UUID event = event();
        UUID activity = activity(event);
        jdbc.update("insert into event_authoring.agenda_item values (?, ?, '2030-01-01T10:00Z', 7200, 'Welcome', null)", UUID.randomUUID(), activity);
        assertThat(jdbc.queryForObject("select owner_subject from event_authoring.event where id = ?", String.class, event))
                .isEqualTo("issuer|opaque-subject");
        assertThat(jdbc.queryForObject("select start_offset_seconds from event_authoring.activity where id = ?", Integer.class, activity))
                .isEqualTo(7200);
    }

    @ParameterizedTest
    @DisplayName("US-15 / BR-07 / BR-08: invalid activity rows are rejected by PostgreSQL")
    @ValueSource(strings = {"ends_at = starts_at", "ends_at = starts_at - interval '1 second'", "capacity = 0",
            "location = null, online_link = null", "location = '  ', online_link = ' '",
            "registration_close = starts_at + interval '1 second'", "title = ' '", "description = ''",
            "visibility = 'UNKNOWN'", "event_id = '00000000-0000-0000-0000-000000000000'"})
    @Transactional
    void rejectsInvalidActivityRows(String change) {
        UUID activity = activity(event());
        assertThatThrownBy(() -> jdbc.update("update event_authoring.activity set " + change + " where id = ?", activity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @DisplayName("US-14 / BR-03: required event metadata and known enums are enforced")
    @ValueSource(strings = {"title = ''", "description = ' '", "organization_ref = ' '", "owner_subject = ''",
            "category = 'UNKNOWN'", "state = 'UNKNOWN'"})
    @Transactional
    void rejectsInvalidEventRows(String change) {
        UUID event = event();
        assertThatThrownBy(() -> jdbc.update("update event_authoring.event set " + change + " where id = ?", event))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("US-16 / BR-07: an agenda item requires an existing activity")
    @Transactional
    void rejectsAgendaWithoutAParent() {
        assertThatThrownBy(() -> jdbc.update("insert into event_authoring.agenda_item values (?, ?, '2030-01-01T10:00Z', 0, 'Welcome', null)", UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID event() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into event_authoring.event
                (id, organization_ref, owner_subject, title, description, category, state, created_at, version)
                values (?, 'org:external/not-a-uuid', 'issuer|opaque-subject', 'Title', 'Description', 'WORKSHOP', 'DRAFT', now(), 0)
                """, id);
        return id;
    }

    private UUID activity(UUID event) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into event_authoring.activity
                (id, event_id, title, description, starts_at, ends_at, start_offset_seconds, end_offset_seconds,
                 location, capacity, visibility, cancelled, created_at)
                values (?, ?, 'Session', 'Description', '2030-01-01T10:00Z', '2030-01-01T11:00Z', 7200, 7200,
                        'Room', 1, 'PUBLIC', false, now())
                """, id, event);
        return id;
    }
}
