package com.ubb.genai.auth.integration;

import com.ubb.genai.auth.domain.UserStatus;
import com.ubb.genai.auth.repository.UserRepository;
import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import com.ubb.genai.events.authoring.ports.AuthoringIdentityPort;
import org.springframework.stereotype.Component;

/** Authoring actor from the server session; only ACTIVE accounts are eligible. */
@Component
public class SessionAuthoringIdentity implements AuthoringIdentityPort {

    private final CurrentUserProvider currentUser;
    private final UserRepository users;

    public SessionAuthoringIdentity(CurrentUserProvider currentUser, UserRepository users) {
        this.currentUser = currentUser;
        this.users = users;
    }

    @Override
    public Actor currentActor() {
        var userId = currentUser.currentUserId().orElse(null);
        if (userId == null) {
            return new Actor(null, false);
        }
        boolean active = users.findById(userId).map(u -> u.getStatus() == UserStatus.ACTIVE).orElse(false);
        return new Actor(userId.toString(), active);
    }
}
