package com.ubb.genai.events.authoring.persistence;

import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import com.ubb.genai.events.authoring.domain.EventState;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AuthoringEventRepository {
  AuthoringEvent save(AuthoringEvent event);

  Optional<AuthoringEvent> findById(UUID id);

  default Optional<AuthoringEvent> findForUpdate(UUID id) { return findById(id); }

  /** Events in the given states, newest first. */
  default List<AuthoringEvent> findAllInStates(Set<EventState> states) {
    throw new UnsupportedOperationException("Listing events is not supported");
  }

  default AuthoringEvent replace(AuthoringEvent event) {
    throw new UnsupportedOperationException("Aggregate replacement is required");
  }
}
