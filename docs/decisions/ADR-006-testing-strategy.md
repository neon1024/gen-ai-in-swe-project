# ADR-006 — Automated testing strategy

## Status and authorization

**Accepted for the development baseline — requesting-student authorization, 2026-11-10.** Source: the user's “PHASE 0.2 Completion — Approved Technical Stack & Phase 0.3 Baseline” instruction. This records the requesting student's authorization, not independent approval by Duarte. Peer/student review remains **Pending student review** and must occur before dependent implementation. Acceptance authorizes this architectural direction; it does not mean the technologies are installed, working or fully specified.

## Context

S1 R11 requires automated unit tests and justified additional levels; the existing contextLoads is not a product test. The initial user list said JUnit 5, while the configured Boot BOM manages Jupiter 6.0.3.

## Alternatives considered

Use Boot-managed JUnit Jupiter versus forcing JUnit 5 and risking compatibility; actual PostgreSQL integration tests versus relying only on mocks/in-memory behavior.

## Decision

Use Boot-managed JUnit Jupiter, Mockito and Spring Boot Test; Testcontainers with PostgreSQL where appropriate. Use Vitest and React Testing Library for frontend behavior and Playwright for critical end-to-end flows. The user explicitly confirmed retaining Boot-managed JUnit without a forced downgrade after the mismatch was reported.

## Consequences and risks

Preserve framework-managed compatible versions. Use US/BR identifiers in tests. Cover authorization between contexts, capacity/FIFO/overlap races, time boundaries, privacy, persistence and error cases. Browser/user validation supplements automated tests. No arbitrary coverage percentage or fabricated results.

## Requirements and issue impact

NFR-005/006; DEL-002/007/008. All stories; especially #31–33 (concurrency), #38–40 (privacy), #49 (time), #51–57 (attendance/metrics).

Issue numbers refer to [the existing issue map](../planning/github-issues-analysis.md) and [backlog](../planning/backlog.md); assignments are unchanged.

## Follow-up and validation

Additional explicit authorization: user reply in this session, 2026-11-10, confirmed the Boot-managed JUnit version. Evidence: [configured Boot 4.1.1 dependency BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom), junit-jupiter.version 6.0.3. M1 must run actual build/tests on a compatible environment; no application test result is claimed here.
