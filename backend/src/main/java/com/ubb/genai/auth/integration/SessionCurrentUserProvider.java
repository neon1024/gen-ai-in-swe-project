package com.ubb.genai.auth.integration;

import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** The caller is the user id stored in the server session at login; empty for a guest. */
@Component
public class SessionCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<UUID> currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(authentication.getName()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
