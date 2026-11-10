package com.ubb.genai.events.authoring.ports;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

/** The real adapter must participate in the caller transaction and agreed locking protocol. */
public interface RegistrationAuthoringPort {
  enum MutationKind { REMOVE, CAPACITY_CHANGE }
  record GuardRequest(UUID eventId, UUID activityId, MutationKind mutationKind) {}
  interface RegistrationContext {
    long confirmedCount();
    boolean hasAnySignup();
    void promoteEligibleWaitlist(int newCapacity, Instant activityStartsAt, Instant now);
  }
  <T> T withLockedActivity(GuardRequest request, Function<RegistrationContext, T> mutation);
}
