# Mid-term Event Authoring slice design

Status: **Accepted for this isolated Event Authoring slice by the requesting student, 2026-11-10**. The user explicitly approved the proposal, minimum setup and section H interpretations. This does not assert independent Duarte review or a shared contract accepted by other owners. Human developer: jose03pedro. Issues: #17–#21. Requirements remain approved; this document does not reopen product scope. Detailed interfaces, schema and API proposals are in the [integration contract](../../integration/event-authoring-contract.md).

## Goal and ownership

Implement independently testable Event Authoring backend and React UI for Draft creation, activity/agenda management, publication and Published editing under FR-EVT-01–05. Do not implement authentication, organizations, signup/FIFO, discovery, notifications or Duarte's lifecycle transitions. Where absent, introduce explicit ports with fail-closed production behavior and test-only doubles; mark affected acceptance pending real integration.

## Approaches reviewed

| Approach | Benefit | Limitation | Recommendation |
|---|---|---|---|
| Feature-isolated Spring authoring aggregate, explicit external ports and React feature, with minimum approved shared test/build setup | Enables real domain/API/persistence/UI work while preserving other students' ownership | Requires a concrete integration contract and setup/temporal policy approval; actual end-to-end acceptance remains incomplete without upstream adapters | Recommended |
| Wait until every M1/M2/signup/discovery dependency is integrated | Uses real upstream implementations directly | Blocks independently executable authoring work that the latest mission explicitly authorizes | Not preferred for isolated tasks |
| Reimplement missing auth/org/signup foundations or silently adopt the Next.js branch | Could appear to make a local demo work | Violates ownership, approved Vite direction and verification integrity | Rejected |

## Components and flow

Feature package com.ubb.genai.events.authoring contains controller/DTO, application service, real domain invariants, repositories and only necessary external ports. Event owns activities and agendas, the current Owner reference, initially the creator, and state/publication/version metadata. IDs are local UUIDs; external user/org IDs are opaque. Authoring DTOs exclude actor/Owner/state mutation. Session → authorization ports → service/aggregate → PostgreSQL transaction → EventView is the proposed flow.

The UI feature uses explicit server responses/versioning, accessible forms and controlled error states, with supplied auth/org/CSRF context. Main has no frontend; the unmerged develop/frontend Next.js starter conflicts with the Vite ADR. Minimal Vite feature setup therefore needs explicit approval and does not overwrite that branch.

## Data, security and business boundaries

Only Draft → Published is owned here; Completed/Cancelled are modeled read-only and other transitions have no authoring endpoint. Org Admin retains owner-equivalent powers; Staff alone may read drafts but cannot publish/edit. Authoritative membership remains external. Missing production adapters never mean “allow” or “zero registrations”.

Use PostgreSQL constraints, Flyway and atomic aggregate mutations; registration-sensitive writes require a same-transaction external guard/promotion contract. Do not implement FIFO locally. Discovery reads committed publication state; no broker/search clone is added. Exact proposals and conflicts requiring approval are in contract sections C–H.

## Test strategy and honest completeness

Cover all 17 story criteria with TDD and named US/BR tests: domain/service, MVC/session/CSRF, PostgreSQL persistence/rollback/concurrency, React forms/client states, and actual browser integration only where runnable dependencies are real. A fixture-driven browser path does not prove production login or membership. #20 search/registration visibility and #21 real promotion/concurrency remain pending until their owners' modules are integrated.

No test is claimed to pass before execution; no production code is written before an observed expected failing test. Preserve the existing context test and use installed JDK 21 explicitly. Test containers must be project-isolated; do not use the unrelated opsrelay PostgreSQL service. Record all setup changes, commands/results, subagents and reviews in independent GenAI entries.

## Approval checklist

- [x] Approve the isolated feature/ports approach and minimum shared dependency, persistence, security and Vite test/build setup listed in the contract; preserve develop/frontend.
- [x] Approve proposed Event/Activity/Agenda schema/API/ID/version contracts as this share's proposal for later team reconciliation, not as a claim of Duarte agreement.
- [x] Decide the complete activity-write policy, agenda interval inclusivity, effective registration-window rules and removal policy in contract H. Do not silently turn these into official requirements.
- [x] Confirm the requesting student’s isolated-slice authorization. Independent ADR peer review has not been confirmed and remains a gate for cross-owner dependent integration; no such integration is claimed. ADR peer review, slice approval and later student review of implementation are distinct; none is inferred from AI review or blanket implementation authorization. Independent implementation review remains Pending student review and no-GenAI protected work remains excluded.

## Execution after approval

Read writing-plans and write one executable plan for all five issues under docs/superpowers/plans/. The user already selected genuine Subagent-Driven Development. Use fresh implementers and independent spec then quality reviewers; fix and re-review before progressing. Maintain a per-issue branch/stacked history from feat/issue-17-jose03pedro-create-draft-event; no implementation on main. The preferred whole-share branch can be an integration path only if consistent with issue-branch policy. Prepare a PR only after applicable verification, with non-closing references for incomplete acceptance.

Current status: requesting-student slice approval received; application implementation Not Started pending executable-plan review. The latest mission explicitly authorizes isolated authoring work without completed M1/M2; it does not authorize bypassing peer review for cross-owner integration. All five issues stay in scope after approval. Missing upstream integrations do not justify dropping independently testable work.
