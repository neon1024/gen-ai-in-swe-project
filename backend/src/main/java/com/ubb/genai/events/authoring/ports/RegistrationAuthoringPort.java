package com.ubb.genai.events.authoring.ports;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

/**
 * Guards Published activity mutations against authoritative registration data.
 *
 * <p>Lock order agreed by event authoring (pending acceptance by the registration owner, issues
 * #31 and #33):
 *
 * <ol>
 *   <li>The caller (event authoring) already holds the Event row lock ({@code PESSIMISTIC_WRITE})
 *       and has validated the root Event version carried in {@link GuardRequest#expectedVersion()}
 *       before invoking the adapter.
 *   <li>The adapter then locks the signup and waitlist rows of the guarded activity.
 *   <li>Participant-level locks, if any, are taken last.
 * </ol>
 *
 * <p>Registration-side flows that also mutate authoring data must take the Event row lock first, so
 * that both modules acquire locks in the same order. The adapter must join the caller transaction
 * and must never commit or roll back independently; any failure must propagate so the caller
 * transaction rolls back authoring and registration writes together.
 */
public interface RegistrationAuthoringPort {
  enum MutationKind { REMOVE, CAPACITY_CHANGE }

  /**
   * @param expectedVersion root Event version the caller validated under the Event row lock
   * @param requestedCapacity new capacity for {@link MutationKind#CAPACITY_CHANGE}; {@code null}
   *     for {@link MutationKind#REMOVE}
   */
  record GuardRequest(UUID eventId, UUID activityId, MutationKind mutationKind,
      long expectedVersion, Integer requestedCapacity) {}

  interface RegistrationContext {
    long confirmedCount();
    boolean hasAnySignup();
    void promoteEligibleWaitlist(int newCapacity, Instant activityStartsAt, Instant now);
  }

  <T> T withLockedActivity(GuardRequest request, Function<RegistrationContext, T> mutation);
}
