# ADR-004 — Sessions, authorization and administrative bootstrap

## Status and authorization

**Accepted for the development baseline — requesting-student authorization, 2026-11-10.** Source: the user's “PHASE 0.2 Completion — Approved Technical Stack & Phase 0.3 Baseline” instruction. This records the requesting student's authorization, not independent approval by Duarte. Peer/student review remains **Pending student review** and must occur before dependent implementation. Acceptance authorizes this architectural direction; it does not mean the technologies are installed, working or fully specified.

## Context

S1 R2 and approved roles require authentication, system/organization/event authorization and context isolation. Accounts require confirmed email. BR-21/25 protect the last administrator.

## Alternatives considered

Server-side sessions versus custom JWT infrastructure; explicit private administrator provisioning versus public or hardcoded privilege creation.

## Decision

Use Spring Security, server-side sessions, secure HttpOnly cookies, CSRF protection for state-changing requests and supported password hashing. Prefer same-origin frontend/API deployment where practical. Use exactly the approved system/organization/event roles; no custom JWT infrastructure without a concrete requirement. Bootstrap guardrails: no hardcoded credentials, default admin password or privilege escalation through public signup.

## Consequences and risks

SPA/session integration must include CSRF-token handling, logout and context-specific authorization. Session cookie stays HttpOnly; any separate CSRF token exposure must follow the framework mechanism. Local HTTP cookie behavior needs an explicit development configuration without weakening deployed defaults. No new role semantics are introduced.

## Requirements and issue impact

NFR-001; FR-ACC-01–05; FR-ORG-01–08; BR-03/18–25. #3/#4/#6/#10–15; #17/#20; #38–40.

Issue numbers refer to [the existing issue map](../planning/github-issues-analysis.md) and [backlog](../planning/backlog.md); assignments are unchanged.

## Follow-up and validation

Proposed bootstrap mechanism (not finalized): an explicit one-time operator-controlled provisioning command outside public signup, taking credentials from an approved secret source and refusing unintended reprovisioning. Concrete invocation/recovery and deactivated-session behavior remain Q-04, blocking #6/admin execution but not this documentation baseline. [Spring Security CSRF guidance](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
