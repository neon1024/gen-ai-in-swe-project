# ADR-008 — GenAI separation and independent student work

## Status and authorization

**Accepted for the development baseline — requesting-student authorization, 2026-11-10.** Source: the user's “PHASE 0.2 Completion — Approved Technical Stack & Phase 0.3 Baseline” instruction. This records the requesting student's authorization, not independent approval by Duarte. Peer/student review remains **Pending student review** and must occur before dependent implementation. Acceptance authorizes this architectural direction; it does not mean the technologies are installed, working or fully specified.

## Context

S1 R13/R14 require documented GenAI-assisted engineering and human responsibility. S4 separately grades each student’s Independent Feature / No-GenAI Implementation. The product baseline does not require a runtime LLM feature.

## Alternatives considered

Deterministic mandatory business functionality with isolated optional AI adapters versus coupling essential workflows to a model service.

## Decision

Mandatory business behavior must work without an LLM, external model API or generated output. Any later justified optional GenAI feature must be isolated; this ADR authorizes no new product feature. Preserve the course’s separate authorship requirement: the designated individual feature must be implemented without GenAI, not merely operate without it.

## Consequences and risks

Runtime independence and no-GenAI authorship are different obligations. The instructor/student must identify the actual independent feature and allowed surrounding assistance under Q-09 before any agent designs or implements it. Do not invent a required non-GenAI product feature or automate its protected work. Record tool/model/effort per task; human review stays distinct.

## Requirements and issue impact

DEL-003–006/009; S1 R13/R14/R16; S4 Independent Feature / No-GenAI Implementation. All product issues must remain usable without runtime GenAI; no dedicated LLM issue exists.

Issue numbers refer to [the existing issue map](../planning/github-issues-analysis.md) and [backlog](../planning/backlog.md); assignments are unchanged.

## Follow-up and validation

Q-09 does not block committing technical documentation, but blocks agent work on the protected feature until its boundary is known. No LLM dependency, API key or GenAI application code is added.
