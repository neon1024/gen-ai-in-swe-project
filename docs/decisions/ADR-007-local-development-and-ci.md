# ADR-007 — Local infrastructure, CI and deployment

## Status and authorization

**Accepted for the development baseline — requesting-student authorization, 2026-11-10.** Source: the user's “PHASE 0.2 Completion — Approved Technical Stack & Phase 0.3 Baseline” instruction. This records the requesting student's authorization, not independent approval by Duarte. Peer/student review remains **Pending student review** and must occur before dependent implementation. Acceptance authorizes this architectural direction; it does not mean the technologies are installed, working or fully specified.

## Context

S2 A requires a configured environment and frontend/backend/database integration; S1 R12 requires reproducible execution beyond an individual machine.

## Alternatives considered

Lightweight local Compose plus GitHub Actions versus manually configured machines or unnecessary cluster infrastructure.

## Decision

Use Docker Compose for required local infrastructure (PostgreSQL and Mailpit as needed), GitHub Actions for automated checks, and preserve suitable existing tooling. Use environment variables or approved secret management. Keep repository artifacts in English. Prefer same-origin UI/API deployment where practical.

## Consequences and risks

No Compose file, workflow, environment file, container image or deployment is created now. M1 pins compatible tools/images, documents actual commands and defines secret-free configuration. Hosting, recovery/retention targets and supported devices remain Q-10 for dependent milestones. Do not commit credentials or create a production system in the preparation phase.

## Requirements and issue impact

NFR-007; TC-001–003; DEL-007. Shared foundation P-04; product issues consume it, with delivery gates P-06/P-07 rather than invented setup issue numbers.

Issue numbers refer to [the existing issue map](../planning/github-issues-analysis.md) and [backlog](../planning/backlog.md); assignments are unchanged.

## Follow-up and validation

Before coordinated implementation, a student must associate shared foundation work with a real issue or explicitly authorize its coordination; no new issue/owner is invented. This is an execution prerequisite, not a reason to reject the documentation baseline.
