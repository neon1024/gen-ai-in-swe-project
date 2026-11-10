package com.ubb.genai.eventlifecycle.shared.port;

import java.util.Optional;
import java.util.UUID;

/** Identifies the caller of the current request; empty for a guest. */
public interface CurrentUserProvider {

    Optional<UUID> currentUserId();
}
