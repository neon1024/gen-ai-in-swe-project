package com.ubb.genai.events.authoring.ports;

/**
 * Resolves trusted server identity and current authoritative account eligibility. No request actor
 * is accepted.
 */
public interface AuthoringIdentityPort {
  record Actor(String subject, boolean eligible) {}

  Actor currentActor();
}
