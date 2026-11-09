# Proposed incremental roadmap

Technical direction: **Accepted by the requesting student**, see ADR-001–008. Incremental task order remains **Proposed**, 2026-11-10. Candidate technical order, without invented dates or effort estimates. GitHub milestones **Mid-term** and **Final** remain unchanged; M0–M7 are local increments, not new remote milestones. Sources: [baseline](../requirements/overview.md), [issues](github-issues-analysis.md) and [decisions](../decisions/README.md). Detailed FR/BR/US criteria remain normative.

## M0 — Approve preparation and resolve first-slice decisions

- Objective/components: review baseline, design, research and ownership; documentation only.
- Requirements: DEL-001–006/008/009; NFR-003–005; TC-001–003.
- Dependencies: no implementation; Q-01/Q-02 technical choices accepted; obtain peer/slice review and delimit no-GenAI work under Q-09 before the affected task.
- Deliverables: dated decisions, approved first-slice design/responsibilities; planned research with actual results added when collected.
- Acceptance/tests: consistency with S1–S4 and human review; no hypothesis presented as an interview.
- Risks: confusing product approval with architecture approval. Advance to M1 only with accepted setup decisions.

## M1 — Verifiable environment and vertical-slice foundation

- Requirements: TC-001–003; NFR-002–004/006/007; DEL-008.
- Components: existing backend, approved frontend/storage; local test configuration.
- Dependencies: M0; accepted ADR-001–008, student review and detailed foundation contract. Q-01/Q-02 are no longer missing-stack blockers. No infrastructure beyond what the first slice requires.
- Deliverables: reproducible environment, initial contract and foundation integration test, documented actual commands; do not replace Hello without a justified task.
- Acceptance/tests: build/test on the agreed JDK, demonstrated UI/API/persistence connection and test data surviving restart; no secrets.
- Risks: observed JDK 27 versus target 21, non-executable wrapper and unavailable dependencies. Advance only with proven execution.

## M2 — Accounts and legitimate organization (Mid-term)

- Requirements: FR-ACC-01–03; FR-ORG-01–04/06/07; BR-19–23; NFR-001/002/004–006; DEL-002/003/004/006/008.
- Components: identity, transactional email, organizations and WF-01 UI.
- Dependencies: M1; administrative bootstrap Q-04; personal/contact email Q-11. Protect Owner departure/removal once events are connected.
- Deliverables: complete US-01–10; account email confirmation, organization approval, controlled membership/roles. Confirmation/reset email starts here; general inbox remains M5.
- Acceptance/tests: every US-01–10 criterion, last-admin invariants and authorization between contexts; confirmation/reset and persistence tests; UX validation.
- Risks: missing bootstrap and dependency on ownership transfer scheduled for Final. Do not silently bring team management forward. Advance with a demonstrated, approved account → organization flow.

## M3 — Publishable event and discovery (Mid-term)

- Requirements: FR-EVT-01–04/07/08; FR-DSC-01–04; BR-01–09/18; US-14–17/22–26.
- Components: events, activities, agenda, discovery and clock; WF-02/WF-03 UI.
- Dependencies: M2; time boundaries Q-05 and roles Q-07. Private activities remain M5; allow only the approved public scope in this slice.
- Deliverables: draft event → publication → search; validated agenda and automatic completion; deletion of drafts only.
- Acceptance/tests: publication eligibility, roles, categories/filters, no draft leaks; dates/agenda and last non-cancelled activity tested; listed story criteria.
- Risks: automatic completion/timing; Editor still lacks public mid-term assignment management. Advance with organizer → discovered event flow and stable contract.

## M4 — Registrations, changes and complete mid-term

- Requirements: FR-REG-01–05; FR-DSC-05; FR-EVT-05/06; BR-04/05/09–14/21–23; US-18/20/21/27–29/31; NFR-001–007; TC-001–003; DEL-001–004/006/008/009.
- Components: registrations, waitlist, changes/cancellations and UI; WF-01–03 integration.
- Dependencies: M3; Q-05/Q-06 and approved transaction policy. General notifications remain Final.
- Deliverables: correct capacity, FIFO and overlap handling; own signups; consistent changes/cancellations; mid-term package (UI/backend/database, research and declarations).
- Acceptance/tests: every listed story criterion and S2 delivery obligation; concurrent last-place requests, simultaneous signups by the same user, rollback, capacity increase, cascading cancellation and refused Owner departure. Demo and UX with safe data.
- Risks: races between activities and state counting. Advance with reviewed mid-term, integration tests and academic evidence, not merely a functional API.

## M5 — Privacy, communication, team and administration (Final)

- Requirements: FR-PRV-01–03; FR-NTF-01–06; FR-TEAM-01–04; FR-ORG-05/08; FR-ACC-04/05; BR-18–25; US-11–13/19/30/32–43.
- Components: invitations/inbox/email, team/tasks, scheduler and administration; WF-01/04/05 UI and WF-02/03 extensions.
- Dependencies: M4; resolve Q-04/Q-06/Q-07; reuse M2 basic email. Within M5: inbox/delivery → invitations/notifications; team → tasks/transfer; scheduler → reminders. Administration can proceed once required invariants exist.
- Deliverables: all listed stories; remote issue changes are not implied.
- Acceptance/tests: private authorization in queries and commands, no duplicate invitations, email failures/retries, alerts for confirmed/waitlisted participants, reminders only for eligible signups at 24h; last admin/Owner protected; deactivation cancels future signups and promotes.
- Risks: private-data leaks, repeated notifications and deactivation with an open session. Advance with verified communication/privacy integration.

## M6 — Execution and follow-up (Final)

- Requirements: FR-ATT-01–03; FR-FBK-01/02; FR-RPT-01/02; BR-15–17/19; US-44–50.
- Components: QR/check-in, feedback and reports; WF-06 UI.
- Dependencies: M5 for complete permissions; M4 for signups; Q-05/Q-08 for time and metrics.
- Deliverables: QR/manual check-in, correction, anonymous feedback and aggregate CSV.
- Acceptance/tests: every story; QR for another activity/cancelled/waitlisted/duplicate signup, -1h/+7d window, unique 1–5 rating until +7d, feedback authorization and report without personal data; approved formula including empty cases.
- Risks: exposed QR and incorrect report denominator. Advance only with demonstrated publication → signup → attendance → feedback → report lifecycle.

## M7 — Validation and final delivery

- Requirements: all FR/BR; NFR-001–007; TC-001–003; DEL-001–009 as applicable to delivery; S1 R1–R16.
- Components: integrated system, documentation, deployment and individual artifacts.
- Dependencies: M6 and accumulated research/evidence; Q-09/Q-10 resolved within delivery scope.
- Deliverables: complete application, reproducible deployment, consistent diagrams/API/data model, test results, actual traceability, GenAI/reflection and contribution tables; students' no-GenAI feature.
- Acceptance/tests: demonstration in another environment, six-workflow regression, academic-criteria review and individual defense; no success without logs/evidence.
- Risks: leaving documentation/testing until the end. Both must accompany M1–M6; M7 consolidates. Complete only with evidence and human delivery approval.

See small tasks in the [backlog](backlog.md). Apply the [collaboration guide](collaborative-development.md) throughout M0–M7: explicit issue/human owner, dedicated branch, approved contracts, per-session GenAI entry and student review before integration. No milestone automatically assigns work to A/B.

Each slice follows design → approval → writing-plans → authorized implementation → verification/review. New optional features follow mandatory commitments. All saved roadmap text, plans, architecture decisions, verification records, logs, commit messages and PR descriptions must be in English.

## Shared baseline handoff

Phase 0.2 technical choices are recorded in the ADRs with requesting-student authorization, without claiming independent Duarte approval. The Phase 0.3 commit/push is explicitly authorized after integrity checks. Application work remains unstarted. [Baseline and kickoff](baseline-and-kickoff.md) distinguishes the first planned issues from currently eligible work and preserves their actual owners.
