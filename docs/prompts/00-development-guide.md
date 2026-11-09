# Sequential development prompt guide

Proposed sequence, not executed. Every development command/path is relative to the Git root; open the repository folder without nesting another folder with the same name. Approving preparation does not automatically approve the stack, architecture or each future execution.

## Repository language policy

Write every saved development prompt, Superpowers-generated plan, architecture decision, GitHub traceability description, GenAI entry, test/verification document, report, PR description and commit message in English. This applies to every model and coding agent. Direct conversational summaries/questions to students may use European Portuguese. Text prepared for copying into the repository must already be in English.

## Common contract for each prompt

Applies to the whole team and every model; follow the [collaboration guide](../planning/collaborative-development.md). jose03pedro and DuarteLagoela work on their assigned issues; do not redistribute. Before implementation, fill the fields below from evidence without inferring owner/model. P tasks without issues can be discussed as documentation; coordinated implementation awaits a human association with a real issue.

## Continuity between sessions

Read `AGENTS.md`, [decisions](../decisions/README.md), [requirements](../requirements/overview.md), [matrix](../requirements/traceability.md), [roadmap](../planning/roadmap.md), [backlog](../planning/backlog.md) and the selected story in `docs/requirements/08-user-stories.md`. Check `rtk git status --short` and preserve local changes. Inspect the issue and confirm live state before starting; the Phase 0 snapshot is historical.

Choose one task with verified dependencies and approved design. For new changes: `using-superpowers` → `brainstorming` and design approval → `writing-plans` → explicitly requested implementation → tests/review → `verification-before-completion`. Read actual available skill instructions; do not assume availability from this machine's temporary paths. Use `systematic-debugging` for bugs and `test-driven-development` where appropriate. `executing-plans`, `requesting-code-review`, `using-git-worktrees` and `finishing-a-development-branch` depend on task/authorization. Delegation/subagents require authorization and are not a preparation prerequisite.

A milestone prompt provides context: select one story/task per execution, without automatically implementing the whole milestone. Do not advance while blocking tests/decisions remain unresolved. Do not change issues, commit, push or merge without applicable authorization. Never generate the feature reserved for individual no-GenAI implementation.

At session end, record task/issue, verified criteria, commands/results, local changes, pending decision/risk, actual code/test/PR links when available, and the next eligible task. Update backlog, matrix, GenAI and contributions. Distinguish verified implementation, ready for integration (approved PR and confirmed human review), integrated (confirmed merge) and issue closed. Story delivery still requires merge under the existing DoD. Do not automatically close issues or merge; use `Refs #` instead of automatic closing in the PR. Report any missing delivery evidence.

## Sequential prompts

Each prompt includes the common documents above and sources S1–S4 as applicable. Referenced story criteria and milestone gates are part of the prompt. Before implementation, request design approval if absent; when a decision is missing, prepare alternatives and stop only dependent work. Do not invent frontend/database commands: document them only after they exist.

### Prompt 00 — M0 / P-01–03 — approve decisions and prepare the first slice

Copy into a future session and identify the concrete task/story where applicable:

> Objective: M0 / P-01–03 — approve decisions and prepare the first slice.
> Issue: state the real #number and URL; inspect current owner/state, PRs and other active implementations. Without an associated issue, limit work to documentation preparation.
> Human developer: state the confirmed issue handle/name; do not assign A/B by model. If no owner exists, record Unassigned and await human selection.
> Branch/worktree: identify your dedicated feature branch and authorized directory/clone; check other people's changes and do not implement on main.
> Allowed components: list this issue's files/components under the approved design; agree on boundaries with the colleague and do not expand scope.
> Dependencies: list predecessor issues with completion evidence, gates and potential conflicts; do not start blocked work.
> ADRs/contracts: identify accepted versions/decisions and shared API/data contracts; significant changes need a proposal, affected issues, compatibility assessment and prior human approval.
> Logging/verification: confirm actual tool, model and effort; create your own UUID entry with actual commands/results in docs/genai-usage/entries/. Unknown model/effort remains Not confirmed for this task. A single integrator consolidates the primary index, without concurrent append.
> Review/integration: AI review is assisted verification, not human review; keep Pending student review until identifiable student confirmation. Require criteria/compatibility review and approved PR; no automatic issue closure or merge.
> Requirements: NFR-003–005; TC-001/002; DEL-001–006/008/009.
> Context: read common documents and docs/architecture/overview.md; docs/decisions/README.md; docs/requirements/06-user-research.md.
> Skills: read and apply using-superpowers; brainstorming; verification-before-completion.
> Scope: Review architecture alternatives, stack, bootstrap and ownership. Record already-approved human decisions; propose options for the rest. Prepare research without inventing interviews. Do not write code or detailed plans for unapproved design.
> Deliverables: Dated decisions, first-slice scope, owners and no-GenAI boundaries.
> Acceptance: Q-01/Q-02 and dependent decisions resolved before M1; hypotheses distinguished from findings.
> Tests/checks: Review sources, IDs and links; confirm no code changes.
> Completion: Finish with explicit open decisions and the next eligible task; await required approval.

### Prompt 01 — M1 / P-04 — verifiable foundation

Copy into a future session and identify the concrete task/story where applicable:

> Objective: M1 / P-04 — verifiable foundation.
> Issue: state the real #number and URL; inspect current owner/state, PRs and other active implementations. Without an associated issue, limit work to documentation preparation.
> Human developer: state the confirmed issue handle/name; do not assign A/B by model. If no owner exists, record Unassigned and await human selection.
> Branch/worktree: identify your dedicated feature branch and authorized directory/clone; check other people's changes and do not implement on main.
> Allowed components: list this issue's files/components under the approved design; agree on boundaries with the colleague and do not expand scope.
> Dependencies: list predecessor issues with completion evidence, gates and potential conflicts; do not start blocked work.
> ADRs/contracts: identify accepted versions/decisions and shared API/data contracts; significant changes need a proposal, affected issues, compatibility assessment and prior human approval.
> Logging/verification: confirm actual tool, model and effort; create your own UUID entry with actual commands/results in docs/genai-usage/entries/. Unknown model/effort remains Not confirmed for this task. A single integrator consolidates the primary index, without concurrent append.
> Review/integration: AI review is assisted verification, not human review; keep Pending student review until identifiable student confirmation. Require criteria/compatibility review and approved PR; no automatic issue closure or merge.
> Requirements: TC-001–003; NFR-002–004/006/007; DEL-008.
> Context: read common documents and backend/pom.xml; backend/.mvn/wrapper/maven-wrapper.properties; accepted ADRs; roadmap M1.
> Skills: read and apply using-superpowers; writing-plans after design approval; test-driven-development; verification-before-completion.
> Scope: Prepare only the approved environment and first UI/API/persistence integration. Preserve the existing backend; do not implement product stories or choose pending technologies.
> Deliverables: Slice technical plan, minimal setup, initial contract, actual commands and reproducible evidence.
> Acceptance: P-04 criteria and M1 gate; compatible JDK/stack and test data surviving restart.
> Tests/checks: Execute actual backend commands and authorized new components, foundation test and diff review; record actual failures.
> Completion: Stop after P-04; update traceability and do not automatically begin US-01.

### Prompt 02 — M2 — accounts and organization

Copy into a future session and identify the concrete task/story where applicable:

> Objective: M2 — accounts and organization.
> Issue: state the real #number and URL; inspect current owner/state, PRs and other active implementations. Without an associated issue, limit work to documentation preparation.
> Human developer: state the confirmed issue handle/name; do not assign A/B by model. If no owner exists, record Unassigned and await human selection.
> Branch/worktree: identify your dedicated feature branch and authorized directory/clone; check other people's changes and do not implement on main.
> Allowed components: list this issue's files/components under the approved design; agree on boundaries with the colleague and do not expand scope.
> Dependencies: list predecessor issues with completion evidence, gates and potential conflicts; do not start blocked work.
> ADRs/contracts: identify accepted versions/decisions and shared API/data contracts; significant changes need a proposal, affected issues, compatibility assessment and prior human approval.
> Logging/verification: confirm actual tool, model and effort; create your own UUID entry with actual commands/results in docs/genai-usage/entries/. Unknown model/effort remains Not confirmed for this task. A single integrator consolidates the primary index, without concurrent append.
> Review/integration: AI review is assisted verification, not human review; keep Pending student review until identifiable student confirmation. Require criteria/compatibility review and approved PR; no automatic issue closure or merge.
> Requirements: FR-ACC-01–03; FR-ORG-01–04/06/07; BR-19–23; US-01–10; NFR-001–006.
> Context: read common documents and UC-01–09; decisions Q-04/Q-11; backlog T-US-01–10; issue #3 as the first candidate.
> Skills: read and apply brainstorming for task design; writing-plans; test-driven-development; requesting-code-review; verification-before-completion.
> Scope: Select the next eligible T-US. The first candidate is account registration/confirmation, not all accounts/organizations. Confirmation/reset email is required now; general inbox stays M5.
> Deliverables: One story with UI/API/data/tests and documentation; evidence and traceability.
> Acceptance: Every story checkbox; denied unauthorized access; last admin protected where relevant.
> Tests/checks: Automated criteria with US/BR; confirmation/reset, uniqueness and non-enumeration; integration and UX validation as applicable.
> Completion: Task verified/reviewed, actual state recorded; M2 gate only after US-01–10 are complete.

### Prompt 03 — M3 — events and discovery

Copy into a future session and identify the concrete task/story where applicable:

> Objective: M3 — events and discovery.
> Issue: state the real #number and URL; inspect current owner/state, PRs and other active implementations. Without an associated issue, limit work to documentation preparation.
> Human developer: state the confirmed issue handle/name; do not assign A/B by model. If no owner exists, record Unassigned and await human selection.
> Branch/worktree: identify your dedicated feature branch and authorized directory/clone; check other people's changes and do not implement on main.
> Allowed components: list this issue's files/components under the approved design; agree on boundaries with the colleague and do not expand scope.
> Dependencies: list predecessor issues with completion evidence, gates and potential conflicts; do not start blocked work.
> ADRs/contracts: identify accepted versions/decisions and shared API/data contracts; significant changes need a proposal, affected issues, compatibility assessment and prior human approval.
> Logging/verification: confirm actual tool, model and effort; create your own UUID entry with actual commands/results in docs/genai-usage/entries/. Unknown model/effort remains Not confirmed for this task. A single integrator consolidates the primary index, without concurrent append.
> Review/integration: AI review is assisted verification, not human review; keep Pending student review until identifiable student confirmation. Require criteria/compatibility review and approved PR; no automatic issue closure or merge.
> Requirements: FR-EVT-01–04/07/08; FR-DSC-01–04; BR-01–09/18; US-14–17/22–26.
> Context: read common documents and UC-10/11/14/15/19; roles; Q-05/Q-07; roadmap M3.
> Skills: read and apply brainstorming; writing-plans; test-driven-development; requesting-code-review; verification-before-completion.
> Scope: Select an unblocked M3 T-US; create an event with activities, publish or query according to the task. Do not implement signups or private activities here.
> Deliverables: Selected story with contract, UI and tests; documented time policy.
> Acceptance: Story checkboxes, consistent permissions/visibility, no publication of invalid data.
> Tests/checks: Temporal tests, publication/agenda, filters and denial; integration with a real organization and controlled clock.
> Completion: Update matrix/evidence; close M3 only after publication→discovery and timed completion are verified.

### Prompt 04 — M4 — registrations and mid-term delivery

Copy into a future session and identify the concrete task/story where applicable:

> Objective: M4 — registrations and mid-term delivery.
> Issue: state the real #number and URL; inspect current owner/state, PRs and other active implementations. Without an associated issue, limit work to documentation preparation.
> Human developer: state the confirmed issue handle/name; do not assign A/B by model. If no owner exists, record Unassigned and await human selection.
> Branch/worktree: identify your dedicated feature branch and authorized directory/clone; check other people's changes and do not implement on main.
> Allowed components: list this issue's files/components under the approved design; agree on boundaries with the colleague and do not expand scope.
> Dependencies: list predecessor issues with completion evidence, gates and potential conflicts; do not start blocked work.
> ADRs/contracts: identify accepted versions/decisions and shared API/data contracts; significant changes need a proposal, affected issues, compatibility assessment and prior human approval.
> Logging/verification: confirm actual tool, model and effort; create your own UUID entry with actual commands/results in docs/genai-usage/entries/. Unknown model/effort remains Not confirmed for this task. A single integrator consolidates the primary index, without concurrent append.
> Review/integration: AI review is assisted verification, not human review; keep Pending student review until identifiable student confirmation. Require criteria/compatibility review and approved PR; no automatic issue closure or merge.
> Requirements: FR-REG-01–05; FR-DSC-05; FR-EVT-05/06; BR-04/05/09–14/21–23; US-18/20/21/27–29/31; P-06.
> Context: read common documents and UC-12/13/16–18; Q-05/Q-06; S2 A–E; approved transaction contracts.
> Skills: read and apply brainstorming; writing-plans; test-driven-development; systematic-debugging for failures; verification-before-completion.
> Scope: Select one unblocked story; protect the last place and participant overlap. Do not bring Final notifications forward. After stories, run P-06 separately.
> Deliverables: Story/concurrency tests, consistent data and matrix; complete mid-term package at P-06.
> Acceptance: Checkboxes, correct capacity/FIFO, consistent cancellations/changes and substantiated S2 delivery.
> Tests/checks: Last-place and cross-activity races, eligible FIFO, rollback, cascading changes, time boundaries; UI→API→database demo and documented real research.
> Completion: Close only verified scope; no mid-term delivery claim without actual research and declarations.

### Prompt 05 — M5 — invitations, communication and team

Copy into a future session and identify the concrete task/story where applicable:

> Objective: M5 — invitations, communication and team.
> Issue: state the real #number and URL; inspect current owner/state, PRs and other active implementations. Without an associated issue, limit work to documentation preparation.
> Human developer: state the confirmed issue handle/name; do not assign A/B by model. If no owner exists, record Unassigned and await human selection.
> Branch/worktree: identify your dedicated feature branch and authorized directory/clone; check other people's changes and do not implement on main.
> Allowed components: list this issue's files/components under the approved design; agree on boundaries with the colleague and do not expand scope.
> Dependencies: list predecessor issues with completion evidence, gates and potential conflicts; do not start blocked work.
> ADRs/contracts: identify accepted versions/decisions and shared API/data contracts; significant changes need a proposal, affected issues, compatibility assessment and prior human approval.
> Logging/verification: confirm actual tool, model and effort; create your own UUID entry with actual commands/results in docs/genai-usage/entries/. Unknown model/effort remains Not confirmed for this task. A single integrator consolidates the primary index, without concurrent append.
> Review/integration: AI review is assisted verification, not human review; keep Pending student review until identifiable student confirmation. Require criteria/compatibility review and approved PR; no automatic issue closure or merge.
> Requirements: FR-PRV-01–03; FR-NTF-01–06; FR-TEAM-01–04; FR-ORG-05/08; FR-ACC-04/05; US-11–13/19/30/32–43.
> Context: read common documents and UC-20–27/31 and extensions; Q-04/Q-06/Q-07 decisions; M2 email adapter.
> Skills: read and apply brainstorming; writing-plans; test-driven-development; requesting-code-review; verification-before-completion.
> Scope: One story per execution; inbox/delivery before invitations/notifications, team before tasks/transfer. Do not introduce an external queue without approved justification.
> Deliverables: Complete story, reused rules, integration tests and failure/retry policy.
> Acceptance: Criteria and BR-18–25; private authorization, no personal email exposure, last admin/Owner protected.
> Tests/checks: Private queries/commands, duplicate/refused invitations, email failure, 24h/late/cancelled reminders, deactivation and promotion.
> Completion: Finish the story and record evidence; M5 gate after all mandatory groups are integrated.

### Prompt 06 — M6 — execution and follow-up

Copy into a future session and identify the concrete task/story where applicable:

> Objective: M6 — execution and follow-up.
> Issue: state the real #number and URL; inspect current owner/state, PRs and other active implementations. Without an associated issue, limit work to documentation preparation.
> Human developer: state the confirmed issue handle/name; do not assign A/B by model. If no owner exists, record Unassigned and await human selection.
> Branch/worktree: identify your dedicated feature branch and authorized directory/clone; check other people's changes and do not implement on main.
> Allowed components: list this issue's files/components under the approved design; agree on boundaries with the colleague and do not expand scope.
> Dependencies: list predecessor issues with completion evidence, gates and potential conflicts; do not start blocked work.
> ADRs/contracts: identify accepted versions/decisions and shared API/data contracts; significant changes need a proposal, affected issues, compatibility assessment and prior human approval.
> Logging/verification: confirm actual tool, model and effort; create your own UUID entry with actual commands/results in docs/genai-usage/entries/. Unknown model/effort remains Not confirmed for this task. A single integrator consolidates the primary index, without concurrent append.
> Review/integration: AI review is assisted verification, not human review; keep Pending student review until identifiable student confirmation. Require criteria/compatibility review and approved PR; no automatic issue closure or merge.
> Requirements: FR-ATT-01–03; FR-FBK-01/02; FR-RPT-01/02; BR-15–17/19; US-44–50.
> Context: read common documents and UC-28–30; Q-05/Q-08; roles and signup states.
> Skills: read and apply brainstorming; writing-plans; test-driven-development; requesting-code-review; verification-before-completion.
> Scope: One attendance, feedback or reporting task with completed predecessors. No certificates, walk-in or personal-data export.
> Deliverables: Story with QR/check-in, feedback or CSV according to its objective, tests and documentation.
> Acceptance: Story criteria; correct windows/authorization; report with agreed formula and anonymity.
> Tests/checks: Invalid/cross-activity/repeated QR, -1h/+7d boundaries, uniqueness/rating/attendance, correction; empty-set metrics and aggregate CSV.
> Completion: Finish the task; M6 complete only after a demonstrable attendance→feedback→report flow.

### Prompt 07 — M7 — validate and deliver

Copy into a future session and identify the concrete task/story where applicable:

> Objective: M7 — validate and deliver.
> Issue: state the real #number and URL; inspect current owner/state, PRs and other active implementations. Without an associated issue, limit work to documentation preparation.
> Human developer: state the confirmed issue handle/name; do not assign A/B by model. If no owner exists, record Unassigned and await human selection.
> Branch/worktree: identify your dedicated feature branch and authorized directory/clone; check other people's changes and do not implement on main.
> Allowed components: list this issue's files/components under the approved design; agree on boundaries with the colleague and do not expand scope.
> Dependencies: list predecessor issues with completion evidence, gates and potential conflicts; do not start blocked work.
> ADRs/contracts: identify accepted versions/decisions and shared API/data contracts; significant changes need a proposal, affected issues, compatibility assessment and prior human approval.
> Logging/verification: confirm actual tool, model and effort; create your own UUID entry with actual commands/results in docs/genai-usage/entries/. Unknown model/effort remains Not confirmed for this task. A single integrator consolidates the primary index, without concurrent append.
> Review/integration: AI review is assisted verification, not human review; keep Pending student review until identifiable student confirmation. Require criteria/compatibility review and approved PR; no automatic issue closure or merge.
> Requirements: All FR/BR; NFR-001–007; TC-001–003; DEL-001–009; P-07–09.
> Context: read common documents and S1 R1–R16; S3/S4; actual documentation/tests; human evidence for the no-GenAI feature.
> Skills: read and apply verification-before-completion; systematic-debugging for defects; requesting-code-review; finishing-a-development-branch only within authorization.
> Scope: Consolidate regression, deployment and documentation. Fix only defects within authorized scope. Do not generate no-GenAI work or publish/merge automatically.
> Deliverables: System demonstrated in another environment; updated documentation/API/data; actual GenAI/reflection and contribution tables.
> Acceptance: P-07/P-09 gates, covered mandatory criteria and substantiated individual defense.
> Tests/checks: Six-workflow regression, authorization, concurrency, failures, deployment reproduction and traceability audit; record commands/results.
> Completion: Report gaps honestly; deliver only with evidence. Publication/merge follows specific authorization.

## First development prompt after approval

Use Prompt 01 only after P-01 and accepted setup decisions. Short text:

> Work in the current Git root. Read AGENTS.md, the collaboration guide, requirements, accepted decisions, architecture and backlog. Confirm the real issue/URL, human owner, branch/worktree, allowed components, dependencies and ADRs; P-04 has no associated issue yet, so without that association prepare documentation only. Record this session in its own UUID entry and keep human review Pending student review. Address only P-04/M1: use writing-plans to prepare a verifiable technical plan for the first UI/API/persistence foundation under the approved stack. Show files, contracts and concrete checks. If design or stack approval is missing, present alternatives and await that decision. Do not implement product functionality or begin US-01; no commits/push. Await plan approval before execution. Write all saved artifacts in English.

Technical direction is now accepted in ADR-001–008 by the requesting student, with independent peer review pending. Exact slice contracts and relevant business questions remain gates. If those decisions remain open, use Prompt 00. After P-04, the first global candidate is issue #3 / T-US-01, assigned to neon1024; jose03pedro and DuarteLagoela must select one of their own eligible issues instead of implementing it, following Prompt 02. Do not execute any of these prompts during Phase 0.

## Approved baseline kickoff

Use the [two independent kickoff prompts](../planning/baseline-and-kickoff.md) for the confirmed associations jose03pedro/Sol 6.1 Medium and DuarteLagoela/Opus 5.5 Medium. #17 and #25 are planned first owned issues, not currently unblocked; do not bypass shared foundation, accounts/organization predecessors or peer review. The requesting student authorizes the preparation baseline commit/push in Phase 0.3, not execution of these prompts or creation of implementation branches.
