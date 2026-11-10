# Decisions and relevant open questions

Record dated 2026-11-10. **Confirmed**: explicit evidence; **Inferred**: supported conclusion; **Proposed**: recommendation without approval; **Unknown**: missing information. A deferred decision continues to block only dependent work.

## Confirmed

| ID | Decision | Evidence | Effect |
|---|---|---|---|
| D-001 | The only root is `gen-ai-in-swe-project/`; `Project/` is supporting material | Explicit user instruction in this session; `git rev-parse --show-toplevel` | Keep all development changes and commands within the root |
| D-002 | `docs/requirements/` is the team-approved baseline; research is pending | Explicit user reply on 2026-11-10 | Preserve FR/BR/US; hypotheses are not interview findings |
| D-003 | Phases 0/0.1 are documentation-only; no implementation, branches/worktrees, commits or push | Phase 0 brief and Phase 0.1 addendum | Stop after preparing and validating artifacts |
| D-004 | Rules apply to the whole team; jose03pedro and DuarteLagoela will work in parallel on assigned issues, with human ownership | User addendum and explicit clarification in this session | Developer A: Sol 6.1/Medium; B: Opus 5.5/Medium are reported configurations; confirm per task |
| D-005 | Coordinate through GitHub issues; independent GenAI entries consolidate into the primary document | Phase 0.1 addendum | No assumed assignments, remote changes or human review |
| D-006 | English is mandatory for repository artifacts across all agents; direct student conversation may use PT-PT | User's Phase 0.1.1 request, 2026-11-10 | Translate prose without changing requirements/evidence; preserve canonical identifiers, names and authoritative originals; apply to generated plans, logs, commits and PR text |
| D-007 | Adopt ADR-001–008 as the technical development baseline | Explicit requesting-student Phase 0.2 Completion instruction, 2026-11-10 | Accepted by the requesting student; independent Duarte review remains Pending student review |
| D-008 | Retain Boot-managed JUnit Jupiter instead of forcing JUnit 5 | Explicit user reply after BOM mismatch was reported, 2026-11-10 | Boot 4.1.1 BOM manages Jupiter 6.0.3; preserve compatible managed versions |
| D-009 | jose03pedro uses Sol 6.1 Medium; DuarteLagoela uses Opus 5.5 Medium | Explicit user reply in this session, 2026-11-10 | Prepare separate prompts for actual assigned issues; still confirm model/effort per task |
| D-010 | Commit and safely push one shared preparation baseline after checks | Phase 0.3 request and latest completion instruction | No force-push/history rewriting; baseline-branch fallback if protected; no implementation branches |
| D-011 | Event Authoring follow-up decisions K-1–K-7 (cancelled activities read-only, Draft removal unguarded, registration guard request and lock order, migration range V2026111010–019, category precedence, frontend host adoption, canonical authoring model) | Requesting student asked the agent to take all decisions, 2026-11-10 | Recorded in [contract section K](../integration/event-authoring-contract.md); K-3 and K-7 bind other owners and remain proposals until they accept |

The earlier phases had no architecture approval. This is superseded by the requesting student’s explicit Phase 0.2 Completion authorization below. Do not claim independent Duarte approval. No application functionality or dependency installation is authorized; a single Phase 0.3 baseline commit and safe push are authorized after validation.

## ADR register

All eight ADRs are **Accepted for the development baseline by the requesting student** on 2026-11-10. They do not assert installed dependencies, complete contracts or independent Duarte approval. Duarte/peer review is **Pending student review** before dependent implementation.

| ADR | Decision | Status |
|---|---|---|
| [ADR-001 — Modular monolith](ADR-001-modular-monolith.md) | Technical direction and stated guardrails | Accepted — requesting student; peer review pending |
| [ADR-002 — Backend and frontend stack](ADR-002-application-stack.md) | Technical direction and stated guardrails | Accepted — requesting student; peer review pending |
| [ADR-003 — PostgreSQL, Flyway and time storage](ADR-003-persistence-and-time.md) | Technical direction and stated guardrails | Accepted — requesting student; peer review pending |
| [ADR-004 — Sessions, authorization and administrative bootstrap](ADR-004-sessions-and-security.md) | Technical direction and stated guardrails | Accepted — requesting student; peer review pending |
| [ADR-005 — REST, validation, OpenAPI and email](ADR-005-api-and-email-contracts.md) | Technical direction and stated guardrails | Accepted — requesting student; peer review pending |
| [ADR-006 — Automated testing strategy](ADR-006-testing-strategy.md) | Technical direction and stated guardrails | Accepted — requesting student; peer review pending |
| [ADR-007 — Local infrastructure, CI and deployment](ADR-007-local-development-and-ci.md) | Technical direction and stated guardrails | Accepted — requesting student; peer review pending |
| [ADR-008 — GenAI separation and independent student work](ADR-008-genai-boundaries.md) | Technical direction and stated guardrails | Accepted — requesting student; peer review pending |

## Baseline versus execution blockers

No mandatory course technology conflicts were found. The only concrete selected-version mismatch, JUnit 5 versus managed Jupiter 6.0.3, was explicitly resolved by the requesting student. Commit/push of the preparation baseline is not blocked by remaining business questions.

Before application execution: peer review and slice design, actual coordination for P-04 shared foundation, compatible version locks/runtime, and verified issue predecessors. Before affected stories: Q-04 bootstrap (#6), Q-05 temporal boundaries, Q-06 re-registration/invitations, Q-07 team/Owner timing, Q-08 report formulas, Q-09 no-GenAI authorship scope and additional artifacts, Q-10 deployment/retention, Q-11 organization contact-email exposure. Q-03 research remains required for delivery and feature justification. Do not close any question merely to claim implementation readiness.

## Questions and proposals for review

| ID | Question / recommendation | Status | Blocks | Required people/evidence |
|---|---|---|---|---|
| Q-01 | Modular monolith and separate React SPA selected | Accepted technical direction, ADR-001 | Detailed slice review before implementation; not a baseline-commit blocker | Requesting-student authorization recorded; peer review pending |
| Q-02 | React/TypeScript/Vite/Router/Tailwind, PostgreSQL/JPA/Flyway, Spring Security sessions/CSRF and SMTP/Mailpit selected | Accepted direction, ADR-002–007 | Exact unconfigured versions, runtime compatibility and slice contracts verified during M1; not a missing-stack blocker | Preserve Java 21/Boot 4.1.1/Maven; user confirmed managed Jupiter; no silent upgrades |
| Q-03 | Conduct research and review H-01–H-14; fragmented tools are a hypothesis, not an observation | Confirmed: research pending | R5 feature justification and deliveries; does not block documentation | Real interviews and team review |
| Q-04 | One-time administrator provisioning mechanism, recovery and deactivated-session handling | Guardrails accepted in ADR-004; concrete mechanism Proposed | #6/admin execution in M2/M5; not documentation baseline | No public-signup privilege, hardcoded credentials or default password; review operator mechanism before implementation |
| Q-05 | Time: timezone, inclusive boundaries, equal timestamps, adjacent intervals and invalid open/close windows | Unknown | Temporal tests M3/M4/M5/M6 | Approved boundary examples; BR-07 requires end strictly after start |
| Q-06 | Re-registration after cancellation and accepting an invitation refused due to overlap/closed registration: close or retain the invitation? | Unknown | M4 registration and M5 invitations | Reconcile BR-10 and UC-22 without erasing history |
| Q-07 | Owner/Editor are needed before team management scheduled for Final: how should roles be demonstrated at mid-term? | Unknown | M3 permission scenarios; Owner departure in M2 | Automatically created Owner may suffice; bringing team/transfer management forward requires approval; no silent milestone changes |
| Q-08 | Attendance rate: does the denominator include Attended, and what value applies with no confirmed participants/ratings? | Unknown | M6 reports | Approved formula and examples consistent with signup states |
| Q-09 | Who owns each complete workflow/additional artifact and the no-GenAI feature boundaries; which configuration was actually used per task? | Unknown | Assignments and individual work; not this preparation | Team/instructor; do not generate the feature before delimiting it |
| Q-10 | Hosting, devices, plausible load, recovery, data retention and delivery calendar | Unknown | Quality targets/deployment; details can wait until dependent work | Team/instructor and user context; no arbitrary targets |
| Q-11 | How does BR-19 apply to the organization's contact email included in its profile? | Unknown | Profile exposure in M2 | Approve the distinction between organizational contact and personal email |

Phase 0.1 query: 50 stories assigned to five accounts; six epics are `Unassigned`. The user confirmed that jose03pedro and DuarteLagoela are the two contributors about to work in parallel on their issues, and instructions apply to the whole team. Do not remove assignments, reduce the team or infer a task's account/model from name order. The initial Phase 0 snapshot preserves the earlier unassigned state. See [collaboration](../planning/collaborative-development.md).

Confirmation/reset email is already required in M2 (US-01/US-02); it does not require bringing every M5 notification forward. This technical order preserves product milestones.

## Recording a resolution

Create an ADR only for a concrete decision: date, status (Proposed/Accepted/Superseded), participants, sources, alternatives, choice, rationale, consequences, affected FR/BR/US/NFR and follow-up tasks. Approve product and architecture changes separately. D-002 does not automatically approve the architecture or roadmap proposals. Historical Q-01–Q-22 from `Project/` use a separate namespace. Write ADRs and supporting artifacts in English.
