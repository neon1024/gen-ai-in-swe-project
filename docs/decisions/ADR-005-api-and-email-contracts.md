# ADR-005 — REST, validation, OpenAPI and email

## Status and authorization

**Accepted for the development baseline — requesting-student authorization, 2026-11-10.** Source: the user's “PHASE 0.2 Completion — Approved Technical Stack & Phase 0.3 Baseline” instruction. This records the requesting student's authorization, not independent approval by Duarte. Peer/student review remains **Pending student review** and must occur before dependent implementation. Acceptance authorizes this architectural direction; it does not mean the technologies are installed, working or fully specified.

## Context

S1 R9 requires documented interfaces. Confirmation/reset email is already needed at Mid-term; general notifications are Final. BR-19 prevents exposing personal emails.

## Alternatives considered

REST/JSON versus alternative transports; shared errors/DTOs versus leaking persistence entities; SMTP abstraction versus mandatory external-provider coupling.

## Decision

Use REST/JSON, DTO boundaries, Jakarta Bean Validation and centralized exception handling. Document API contracts through a compatible OpenAPI implementation. Select Spring Mail/SMTP behind a configurable service abstraction and Mailpit for local email testing. No hardcoded SMTP credentials or mandatory external provider locally.

## Consequences and risks

Exact endpoint paths, error envelope/status mapping, DTO fields, CSRF exchange and API evolution are shared contracts to agree before a consuming slice. Do not invent public routes as already accepted. Deliver email only with an explicit failure/retry policy; failures must not silently corrupt committed business changes. General inbox/email notifications remain their approved milestones.

## Requirements and issue impact

NFR-004; FR-ACC-01/03; FR-NTF-01–06; BR-19. #3/#4/#22/#34/#37/#39–42/#48/#49.

Issue numbers refer to [the existing issue map](../planning/github-issues-analysis.md) and [backlog](../planning/backlog.md); assignments are unchanged.

## Follow-up and validation

Candidate implementation: springdoc-openapi, whose [official compatibility matrix](https://springdoc.org/) lists Boot 4.1.x with springdoc 3.1.x–3.2.x. Pin/test an exact compatible artifact in M1; this baseline installs none. Provider, retry details and exact UI component library are later choices, not baseline blockers.
