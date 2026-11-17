package com.ubb.genai.organizations.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActiveEventOwnerRepository {

    private final JdbcTemplate jdbc;

    public ActiveEventOwnerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasActiveOwnedEvents(UUID organizationId, UUID userId) {
        Boolean exists = jdbc.queryForObject("""
                select exists (
                    select 1
                    from event_authoring.event
                    where organization_ref = ?
                      and owner_subject = ?
                      and state in ('DRAFT', 'PUBLISHED')
                )
                """, Boolean.class, organizationId.toString(), userId.toString());
        return Boolean.TRUE.equals(exists);
    }
}
