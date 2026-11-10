package com.ubb.genai.events.authoring.ports;

/**
 * Resolves current membership in the exact opaque organization reference; NONE means no membership.
 */
public interface OrganizationAccessPort {
  enum Role {
    NONE,
    MEMBER,
    ORGANIZER,
    ORG_ADMIN
  }

  Role forActor(String subject, String organizationRef);
}
