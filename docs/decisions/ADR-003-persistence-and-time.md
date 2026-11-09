# ADR-003 — PostgreSQL, Flyway and time storage

## Status and authorization

**Accepted for the development baseline — requesting-student authorization, 2026-11-10.** Source: the user's “PHASE 0.2 Completion — Approved Technical Stack & Phase 0.3 Baseline” instruction. This records the requesting student's authorization, not independent approval by Duarte. Peer/student review remains **Pending student review** and must occur before dependent implementation. Acceptance authorizes this architectural direction; it does not mean the technologies are installed, working or fully specified.

## Context

S1 R7 requires persistent storage and modeled constraints. BR-07–14 require temporal/capacity/registration integrity.

## Alternatives considered

PostgreSQL versus another relational store or in-memory persistence; versioned Flyway migrations versus automatic ORM schema updates.

## Decision

Use PostgreSQL, Spring Data JPA/Hibernate and Flyway versioned migrations. Preserve integrity through appropriate constraints and transactions. Do not use automatic destructive schema updates. Store instants in UTC; retain scheduling timezone information where needed.

## Consequences and risks

A PostgreSQL timestamp with time zone normalizes an instant but does not retain its original named timezone; store scheduling zone information separately when required. Choose exact schema and concurrency protection in the relevant slice. Capacity and same-participant overlap races both require transaction tests. UTC storage does not decide inclusive boundaries or adjacent-interval policy (Q-05).

## Requirements and issue impact

NFR-002; S1 R7; BR-07–14. #17–21 (event/activity data), #31–33 (signup/FIFO), #51–57 (attendance/reporting).

Issue numbers refer to [the existing issue map](../planning/github-issues-analysis.md) and [backlog](../planning/backlog.md); assignments are unchanged.

## Follow-up and validation

M1 selects compatible PostgreSQL/Flyway versions and verifies migration/restart behavior. M4 specifies locking/transaction semantics. No schema or migration is created now. Technical source: [PostgreSQL date/time types](https://www.postgresql.org/docs/current/datatype-datetime.html); [Spring Boot SQL support](https://docs.spring.io/spring-boot/reference/data/sql.html).
