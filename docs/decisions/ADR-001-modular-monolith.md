# ADR-001 — Modular monolith

## Status and authorization

**Accepted for the development baseline — requesting-student authorization, 2026-11-10.** Source: the user's “PHASE 0.2 Completion — Approved Technical Stack & Phase 0.3 Baseline” instruction. This records the requesting student's authorization, not independent approval by Duarte. Peer/student review remains **Pending student review** and must occur before dependent implementation. Acceptance authorizes this architectural direction; it does not mean the technologies are installed, working or fully specified.

## Context

S1 R8 requires separated concerns, not independently deployed services. The existing Spring backend is a small scaffold; five GitHub owners contribute product workflows.

## Alternatives considered

Layered modular monolith versus a server-rendered monolith or independent services. Separate services add transaction/operational complexity without a demonstrated requirement.

## Decision

Use the existing Spring Boot backend as a modular monolith, organized primarily by business feature. Inside each feature maintain controller, service, persistence and DTO boundaries. Use a separate React SPA communicating through REST/JSON. Do not introduce microservices, Kubernetes or speculative infrastructure.

## Consequences and risks

Keep authorization and transaction rules in services; controllers adapt transport and repositories persist data. Modules consume agreed service/contracts rather than bypassing another feature’s invariants. This supports local transactions but requires coordinated changes to shared identity, event and signup contracts.

## Requirements and issue impact

NFR-003/004; TC-002; S1 R8/R9. All product issues; especially #17–26 (events), #31–35 (registrations) and #51–57 (follow-up).

Issue numbers refer to [the existing issue map](../planning/github-issues-analysis.md) and [backlog](../planning/backlog.md); assignments are unchanged.

## Follow-up and validation

M1 establishes feature boundaries and a first integration contract; concrete package/class names follow a slice design. Package root remains com.ubb.genai. No module or frontend code is created by this ADR.
