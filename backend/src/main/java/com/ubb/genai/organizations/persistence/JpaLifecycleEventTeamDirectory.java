package com.ubb.genai.organizations.persistence;

import com.ubb.genai.eventlifecycle.shared.port.EventRole;
import com.ubb.genai.eventlifecycle.shared.port.EventTeamDirectory;
import com.ubb.genai.events.authoring.ports.EventTeamAccessPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JpaLifecycleEventTeamDirectory implements EventTeamDirectory {

    private final EventTeamAccessPort eventTeamAccess;
    private final JdbcTemplate jdbc;

    public JpaLifecycleEventTeamDirectory(EventTeamAccessPort eventTeamAccess, JdbcTemplate jdbc) {
        this.eventTeamAccess = eventTeamAccess;
        this.jdbc = jdbc;
    }

    @Override
    public Optional<EventRole> roleOf(UUID eventId, UUID userId) {
        String owner = jdbc.query("""
                select owner_subject
                from event_authoring.event
                where id = ?
                """, result -> result.next() ? result.getString(1) : null, eventId);
        if (userId.toString().equals(owner)) {
            return Optional.of(EventRole.OWNER);
        }
        return switch (eventTeamAccess.roleFor(userId.toString(), eventId)) {
            case EDITOR -> Optional.of(EventRole.EDITOR);
            case STAFF -> Optional.of(EventRole.STAFF);
            case NONE -> Optional.empty();
        };
    }
}
