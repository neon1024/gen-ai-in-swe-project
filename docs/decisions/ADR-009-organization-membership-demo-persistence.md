# ADR-009 — Organization membership persistence and demo identity

## Status and authorization

**Accepted for the local US-06–US-10 demonstration by All16, 2026-11-16; implementation requested to continue on 2026-11-17.** The requesting student selected PostgreSQL persistence, seeded demo data, the existing `X-User-Id` seam limited to local demonstration, and minimal event-team storage/cleanup without event-team management UI. Student/peer review remains **Pending student review**. This scoped decision does not approve the header identity for production or override ADR-004's session-based security direction.

## Context

US-06–US-10 require persistent organization membership and join requests, role management, and member departure/removal. BR-21 requires at least one Org Admin; BR-22 removes event-team assignments with membership; BR-23 prevents departure/removal of an Owner with a draft or published event. Organization-registration and user-account stories are not implemented in this slice.

## Alternatives considered

An in-memory/mock-only membership service would not persist requests or roles across browser sessions. Full user authentication and event-team administration would pull unfinished teammate-owned stories into this slice.

## Decision

Store organizations, memberships, join requests, and the minimum event-team membership cleanup data in PostgreSQL under the `organizations` schema, managed by the shared Flyway history. Provide seeded demonstration organizations and UUID identities. Reuse the current `X-User-Id` provider only to support local demonstration, with a browser selector explicitly labelled as demo-only. Replace the organization and event-team directory stubs with persistence-backed adapters. Implement only membership-related event-team cleanup and active-event ownership checks; do not add event-team assignment administration.

All membership mutations lock the organization row. Event authoring acquires the same lock when authorizing write transactions, preventing a new draft from racing a member's leave/removal. Enforce duplicate pending requests with a partial unique index and enforce the last-admin rule in the same transaction. Read-only admin endpoints must not acquire write locks.

## Consequences and risks

The demo header is impersonable and grants no production identity assurance. The demo selector and seed identities must not be used as deployed authentication. Before deployment, integrate a trusted authenticated principal and review the endpoint security chain. Future event-team management must enforce organization membership when assigning Editor/Staff roles. Ownership transfer remains outside this slice; active event owners are blocked from leaving or removal until a separate approved transfer flow exists.

## Requirements and issue impact

US-06–US-10, FR-ORG-04/06/07, BR-21/22/23; issues #8–#12. The API and data contract is [organization-membership-contract.md](../integration/organization-membership-contract.md).

## Follow-up and validation

Run backend tests, Flyway migrations, Hibernate validation, and HTTP behavior against Java 21 and PostgreSQL before claiming the implementation is verified. Complete student review and production-authentication integration before deployment.
