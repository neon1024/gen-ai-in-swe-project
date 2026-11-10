package com.ubb.genai.events.authoring.persistence;

import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import java.util.Optional;
import java.util.UUID;

public interface AuthoringEventRepository {
  AuthoringEvent save(AuthoringEvent event);

  Optional<AuthoringEvent> findById(UUID id);
}
