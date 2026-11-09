# Collaborative development

## People, agents and scope

These rules apply to the whole team and every agent/model. On 2026-11-10 the user confirmed that **jose03pedro and DuarteLagoela will implement their already-assigned issues in parallel**; they do not replace other GitHub owners. Do not redistribute work automatically. Ownership belongs to the human and remains when the model changes.

Configurations reported in the addendum: Developer A — **Sol 6.1 / Medium**; Developer B — **Opus 5.5 / Medium**. These are student reports, not automatic detection or proof of the model used in this or another task. The user subsequently confirmed jose03pedro → Sol 6.1 Medium and DuarteLagoela → Opus 5.5 Medium. Still confirm the actual configuration at each session’s start; if unknown, record `Not confirmed for this task`.

The [coordination snapshot](github-coordination-snapshot.json) contains 56 issues: 50 stories assigned to five accounts and six `Unassigned` epics. The initial Phase 0 snapshot remains historical; confirm live status, owners, open PRs and in-progress work before starting. Local snapshots are not distributed locks.

All repository artifacts must be in English, including saved prompts, plans, code/comments/tests, logs, review evidence, commit messages and PR descriptions. Direct student conversation may use European Portuguese. Both agents follow the same AGENTS.md; CLAUDE.md only points to it.

## Work unit and human reservation

The primary identifier is the real issue number/URL. US/FR/BR provide product traceability; T-US/P are retained local aliases, not another work queue. The [backlog](backlog.md) records owner, milestone, dependencies, branch, status, verification and PR. P tasks without issues are documentation proposals, not invented GitHub identifiers; obtain association with a real issue before coordinating implementation through them.

Before implementation, the student confirms their assigned issue, own branch/worktree, ADRs, requirements, contracts, verified predecessors and allowed files. Review open PRs/issues to avoid duplication. An assignment does not prove active implementation. For an `Unassigned` issue, await human selection; do not self-assign. If someone else is implementing it, obtain explicit coordination before contributing. Students may publish reservation confirmation on the issue/PR; the agent publishes or changes state/assignee only when explicitly authorized.

Each session records human owner, issue, files/components to modify and consumed contracts. Treat selection as an agreement between students; reconfirm stale state. In local records, `Unknown`/`Not recorded` means missing evidence, not absence of remote work.

## Branches, clones and worktrees

For future implementation use `feat/issue-<number>-<handle>-<summary>` or `fix/issue-<number>-<handle>-<summary>`, confirming the name per task. These are naming conventions, not existing branches. Avoid direct development on main. Phase 0.1 only updated documentation on the existing branch; no branches/worktrees were created.

On one machine, recommend one worktree per student/branch in an authorized location. On different machines, a local clone per person and feature branches suffice. Read `using-git-worktrees` when applicable and inspect the environment before creating a worktree. No agent should use the colleague's working directory or modify their uncommitted files.

Do not automatically merge, rebase, reset, cherry-pick or force-push another person's branch. Preserve history and changes; resolve conflicts on your own branch in coordination with the colleague, without automatically choosing one side.

## Parallel work and shared boundaries

Use actual backlog predecessors and approved contracts; do not invent dependencies to balance workload. Review the graph before execution: Phase 0 technical proposals can be refined with justification. Respect milestone gates. Different workflow labels do not prove independence; stories within one workflow may touch distinct files.

Conditional recommendations for the upcoming jose03pedro/DuarteLagoela collaboration:

| Issues and GitHub owners | Minimum backlog dependencies | Parallel-work potential / conflicts |
|---|---|---|
| #18 US-15, jose03pedro; #25 US-22, DuarteLagoela | Both require #17 (draft); #18 only after approved M2 and M3 design | Candidates after Event/Activity contract approval: activity editing versus draft deletion. Both touch Event/persistence; agree on distinct files and test cascading deletion. Serialize changes to the same file |
| #19 US-16, jose03pedro; #25 US-22, DuarteLagoela | #19 depends on #18; #25 on #17 | Agenda versus draft deletion; parallel work requires stable schema/contract and review of agenda deletion |
| #21 US-18, jose03pedro; #23 US-20, DuarteLagoela | #21 depends on #33 (promotion); #23 on #31 (signup), with M4 eligible | Dependencies may be satisfied together, but changes/cancellation affect the same signups and transactions; coordinate shared contract/implementation first, often serializing |

Cross-cutting files such as pom.xml, configuration, authentication policies, schema/migrations, API contracts, AGENTS, matrices and backlog require coordination before concurrent edits. Prefer PRs limited to their own slice, tests and GenAI entry. This also applies to documentation: a single integrator updates backlog/index at the gate, or contributors agree on sections and reconcile before merge. Worktrees isolate changes; they do not eliminate integration conflicts.

## Common architecture and contracts

Before implementation, read accepted ADRs, FR/BR/US, API/data contracts and related issues/PRs. To change a contract or introduce a significant decision, prepare a short proposal with requirement/need, consuming issues, compatibility, migration if applicable, alternatives and integration tests. Obtain explicit human approval before implementing the change. One owner implements the shared change; update ADRs/contracts after approval and inform consumers through authorized channels. Do not establish incompatible conventions independently in each agent.

## PRs, review and conflicts

Open a PR only when authorized, with `Refs #<number>` and FR/BR/US, change summary, affected contracts/files, executed tests/results, GenAI entry and open items. Avoid automatic-closing words such as `Closes`/`Fixes` without authorization to close the issue. PRs provide integration, without automatic merging. Write PR descriptions and commit messages in English.

The other student reviews code, criteria, tests, compatibility and documentation. AI review supplements but does not replace student review. Record the human reviewer and actual confirmation/evidence; default status is `Pending student review`. The log author must not invent colleague approval. For conflicts, preserve both contributions, agree on a resolution with the authors and rerun affected tests before approval.

## States and Definition of Done

Separate `Not started`, `In progress` (confirmed by a person/PR), `Blocked` (with reason), `Implementation verified` (evidenced local checks), `Ready for integration` (human review and approved PR), `Integrated` (confirmed merge) and `Issue closed` (confirmed by GitHub). These are local coordination states, not automatic GitHub changes. If only issue openness is known, record `Open on GitHub; development status not confirmed`.

Readiness requires satisfied criteria/requirements, relevant passing tests, assessed compatibility, updated documentation/traceability, recorded GenAI usage, completed human review and reviewed/approved PR. Existing story baseline retains merge for delivery; readiness is not delivery. Issue closure and merge are separate human actions or require explicit authorization. Agent-run tests, static analysis, debugging and another agent's review are **AI-assisted verification**. Student-performed manual tests, decisions and acceptance are **human verification**, recorded only when confirmed.

## GenAI records without concurrent edits

The primary document is [docs/genai_usage_log.md](../genai_usage_log.md). Each significant task/session has an entry in `docs/genai-usage/entries/`, based on the [template](../genai-usage/entry-template.md):

`<YYYYMMDDTHHMMSSZ>-issue-<number-or-none>-<handle-or-unassigned>-<uuid4>.md`

A complete UUID reduces collisions even in simultaneous sessions. The name uses UTC (`Z`); the content includes an offset-bearing timestamp and IANA timezone. For several issues, record all and use the primary issue in the filename. Do not use a model-specific shared filename. Each agent edits only its own session entry; new sessions create new UUIDs. Do not store secrets or unnecessary personal data in saved prompts.

Required fields: timestamp/timezone; human; tool/environment; model; effort; prompt/intent; issues; requirements; branch/PR when known; files; commands/checks actually executed; results; human decisions/interventions; human review; outcome and limitations. Unknown fields must be explicit. Use reported A/B configurations only when confirmed for the task. Entries and reflections must be in English; preserve canonical metadata and historical evidence.

**Consolidation:** agents do not edit the index during parallel work. At the integration gate, one authorized student/integrator runs from their branch after collecting integrated entries:

```bash
rtk proxy python3 docs/genai-usage/consolidate.py
rtk proxy python3 docs/genai-usage/consolidate.py --check
```

The script uses only stdlib, sorts by timestamp/id and generates the primary index linking the records; it neither changes entries nor invents metadata. It references historical Phase 0 logs. If PRs have conflicting generated indexes, collect all entry files and regenerate once; do not discard entries. Review the diff, including removal of previously indexed entries. `--check` is read-only and detects a stale index. The final report includes human reflection on effectiveness/limitations based on evidence; the index does not fabricate it.

## Remaining decisions

Architecture/stack direction is accepted by requesting-student authorization in ADR-001–008, with independent peer review pending. Remaining slice/version details and policies Q-03–11; no-GenAI feature boundaries; additional-artifact ownership; per-session confirmation of human/model/effort; actual branch/worktree and files for the selected issue; human selection of reviewer/integrator. Reported model associations are not needed to apply common rules. Existing GitHub assignments remain unchanged and do not authorize an agent to begin an issue during this phase.

## Authorized preparation baseline

The requesting student explicitly authorized one verified Phase 0.3 documentation baseline commit and safe push, with a baseline-branch fallback if direct main push is protected. This supersedes earlier phase-specific no-commit restrictions for this baseline only. It does not authorize application work, automatic PR merge/issue closure or implementation branches. Follow [baseline and kickoff](baseline-and-kickoff.md) for actual first-issue eligibility and separate prompts.
