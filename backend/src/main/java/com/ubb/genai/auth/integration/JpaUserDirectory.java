package com.ubb.genai.auth.integration;

import com.ubb.genai.auth.domain.User;
import com.ubb.genai.auth.repository.UserRepository;
import com.ubb.genai.organizations.application.UserDirectory;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Display names for other modules, read from the user accounts. */
@Component
public class JpaUserDirectory implements UserDirectory {

    private final UserRepository users;

    public JpaUserDirectory(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> displayNames(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName));
    }
}
