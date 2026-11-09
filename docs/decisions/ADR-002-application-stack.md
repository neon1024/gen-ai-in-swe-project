# ADR-002 — Backend and frontend stack

## Status and authorization

**Accepted for the development baseline — requesting-student authorization, 2026-11-10.** Source: the user's “PHASE 0.2 Completion — Approved Technical Stack & Phase 0.3 Baseline” instruction. This records the requesting student's authorization, not independent approval by Duarte. Peer/student review remains **Pending student review** and must occur before dependent implementation. Acceptance authorizes this architectural direction; it does not mean the technologies are installed, working or fully specified.

## Context

The repository declares Java 21, Spring Boot 4.1.1, Maven wrapper 3.3.4 and Maven 3.9.16. JPA is commented out; frontend does not yet exist.

## Alternatives considered

Preserve the existing Java/Maven ecosystem versus replacing it. React/TypeScript SPA versus templates was previously undecided; the requesting student selected the SPA.

## Decision

Preserve compatible configured Java/Spring Boot/Maven versions. Select Spring Web, Data JPA/Hibernate, Jakarta Bean Validation, Spring Security, centralized exception handling and compatible OpenAPI documentation. Select React, TypeScript, Vite, React Router and Tailwind CSS in frontend/. Prefer a consistent accessible component system; do not add a state framework without need.

## Consequences and risks

Only the existing scaffold is installed. Select and lock compatible frontend/Node/database/tool versions in M1 with evidence; do not claim untested compatibility or silently update Boot. OpenAPI implementation follows ADR-005. A component library is not yet selected. The observed host JDK 27 does not replace Java target 21.

## Requirements and issue impact

TC-001/002; NFR-003–006. #3/#4 (accounts), #17/#18/#19 (event UI/API), #25/#28/#29 (deletion/discovery).

Issue numbers refer to [the existing issue map](../planning/github-issues-analysis.md) and [backlog](../planning/backlog.md); assignments are unchanged.

## Follow-up and validation

Maven Central HEAD checks returned HTTP 200 for the configured Boot parent and Maven archive; this checks availability, not a build. No dependency installation or pom.xml/frontend change in this phase.
