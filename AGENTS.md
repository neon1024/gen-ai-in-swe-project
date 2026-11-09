# Project instructions

## Repository Language Policy
English is mandatory for every repository artifact, including instructions, documentation, saved prompts, skill-generated plans, ADRs, code, identifiers, comments, tests, logs, reports, commit messages and PR descriptions. All agents and models must follow this policy. Do not introduce Portuguese prose into repository files or text intended for them. Preserve canonical identifiers, technical terms, proper names and authoritative original sources. Direct conversation with students may use European Portuguese (PT-PT).

## Project Overview
Event management platform for UBB student organizations, with activities, registrations, waitlists and follow-up. Phase 0 prepares documentation; it does not authorize implementation. These rules apply to the entire team and every agent. jose03pedro and DuarteLagoela will work in parallel on their assigned issues; ownership belongs to the person. Reported configurations do not confirm the model used in any session. The user confirmed team approval of the product baseline on 2026-11-10; user research remains pending.

## Sources of Truth
- Academic obligations: [preserved original sources](docs/sources/README.md), S1 R1–R16, S2/S3 deliveries, S4 evaluation.
- Approved product: [requirements](docs/requirements/02-requirements.md), [roles](docs/requirements/03-actors-and-roles.md), [use cases](docs/requirements/04-use-cases.md), [stories and acceptance criteria](docs/requirements/08-user-stories.md).
- Execution: GitHub issues referenced in the [backlog](docs/planning/backlog.md). The GitHub number/URL is the primary identifier; T-US/P are local aliases. An open issue does not prove implementation. Follow the [collaboration guide](docs/planning/collaborative-development.md); preserve existing GitHub assignments without inferring A/B from models.
- [Decisions](docs/decisions/README.md) and [architecture](docs/architecture/overview.md): only explicitly accepted decisions authorize technical choices. Record and review conflicts; never silently replace the assignment or requirements. Historical Markdown in `Project/` is supporting material, not another development baseline.

## Repository Structure
This Git directory is the root. All commands below start here. `backend/`: existing scaffold; `docs/requirements/`: product; `docs/sources/`: originals; `docs/architecture/`: accepted direction and slice proposals; `docs/decisions/`: decisions; `docs/planning/`: audit, roadmap, tasks and evidence; `docs/prompts/`: continuity. Do not create another `gen-ai-in-swe-project/` directory here. Do not write outside the root without authorization.

## Tech Stack
Confirmed in code: Java target 21, Spring Boot 4.1.1, Maven wrapper 3.3.4 selecting Maven 3.9.16, Web MVC, Validation, Lombok, DevTools and Spring/JUnit tests. JPA is commented out. Authorized for future implementation: React/TypeScript/Vite/React Router/Tailwind in frontend/, PostgreSQL with JPA/Hibernate and Flyway, Spring Security server sessions/CSRF, Spring Mail/SMTP with Mailpit locally, Docker Compose and GitHub Actions. These are selected, not installed. Testing uses Boot-managed JUnit Jupiter (6.0.3 in the configured BOM, explicitly confirmed by the requesting student), Mockito, Spring Boot Test, Testcontainers where appropriate, Vitest/React Testing Library and Playwright. Hosting and unconfigured exact versions remain to verify in M1/dependent work. The JDK observed in Phase 0 was 27; this does not prove compatibility with target 21.

## Architecture & Conventions
The feature-oriented modular monolith plus separate React SPA is accepted for the development baseline by the requesting student; follow [ADR-001–008](docs/decisions/README.md). Do not claim independent Duarte approval: student review of these ADRs remains pending before dependent implementation. Slice contracts/designs still need applicable approval. Apply KISS, YAGNI, DRY and separation of concerns. Respect the existing `com.ubb.genai` package. Isolate business rules from transport and persistence; agree on contracts, transactions, errors and logging in each slice's design. Do not log passwords, tokens, personal QR codes or unnecessary email addresses. Avoid speculative abstractions and dependencies.

## Agent Workflow
1. Read these instructions, applicable instructions and task documentation.
2. Identify FR/BR/US and acceptance criteria, plus applicable NFR/TC/DEL.
3. Confirm design approval; resolve the slice's blockers.
4. Discover and read relevant skills in the current environment.
5. Consult the backlog and produce a technical plan only after design approval.
6. Implement only the authorized task; protect the individual no-GenAI feature.
7. Execute appropriate checks and record commands, results and limitations.
8. Review the diff and regressions; obtain the student review required by the stories.
9. Update documentation, traceability and GenAI/contribution evidence.
10. Deliver a verifiable summary; stop at the requested boundary. Phases 0/0.1/0.1.1 did not authorize commits/push. The requesting student explicitly authorizes one preparation baseline commit and safe push in Phase 0.3 after validation, including a baseline-branch fallback if direct integration push is protected. No application implementation or implementation branch creation is authorized in this task.

## Collaboration & Git
Before implementation, confirm issue, human owner, dependencies, branch/worktree, allowed files, ADRs/contracts and in-progress issues. Use a dedicated branch per issue; avoid development on main. On the same machine, recommend separate worktrees; on different machines, separate clones and branches suffice. Do not create branches/worktrees during this documentation phase. Do not touch another person's uncommitted changes or duplicate an active issue without explicit coordination. Agree on file ownership and shared changes (API, database, auth, configuration) before parallel work. Propose contract changes with affected issues, compatibility implications and human approval; then update ADRs/contracts. Do not automatically merge, rebase, reset, cherry-pick or force-push another person's branch. Integrate through PRs with student review and applicable authorization; no automatic issue closure.

## GenAI Logging & Human Review
The primary document is [docs/genai_usage_log.md](docs/genai_usage_log.md). Each session creates its own entry with timestamp/timezone, UUID, issue and human owner, following the guide; never infer model/effort from earlier sessions. Record only checks actually executed and confirmed human interventions. Agents must not append concurrently to the index: one integrator consolidates at the integration gate. Tests, static analysis and another agent's review are AI-assisted verification, not student review. Default human status: `Pending student review`; change it only with explicit, identifiable confirmation.

## Implementation Rules
Do not add features without requirements or approval, change unrelated components, perform unnecessary refactors, introduce unjustified dependencies or silently change public contracts. Do not ignore failures, store secrets, delete existing work or execute destructive actions without authorization. Do not claim success without evidence. Preserve existing FR/BR/US IDs; do not reuse historical IDs from `Project/`. Do not modify issues or message third parties merely to analyze their work.

## Testing & Quality
Existing commands, relative to the root and with the RTK prefix required by the environment:
- `rtk proxy bash backend/mvnw -f backend/pom.xml test`
- `rtk proxy bash backend/mvnw -f backend/pom.xml verify`
- `rtk proxy bash backend/mvnw -f backend/pom.xml spring-boot:run`
The wrapper is not executable; invoke it with `bash`. These commands were identified, not executed in Phase 0. They may download Maven/dependencies and write to the user cache: respect the environment's write authorization. No lint, independent type-checking or frontend test command is configured. Do not invent commands.
Future tests: unit tests for BRs; authorization, persistence and concurrency integration tests; API contracts and complete UI workflows. Use TDD where appropriate; include US/BR IDs according to the existing convention. See the [strategy](docs/architecture/overview.md).

## Definition of Done
Acceptance criteria and requirements satisfied, approved design respected, relevant tests passing, integration compatibility assessed, regressions reported, documentation/traceability and GenAI records updated. Human review and PR approval require explicit confirmation. Distinguish locally verified implementation, ready for integration, integrated and issue closed: these are separate states. Existing story DoD retains peer review and merge for delivery; readiness does not authorize integration or closure. Do not automatically close issues or merge. Commit/push requires applicable authorization; the Phase 0.3 baseline has explicit authorization after validation, without authorizing application work.

## Documentation Maintenance
FR/BR changes require coordinated review of use cases, stories, workflows and both traceability dimensions. Design changes require updated architecture and a dated decision. Each slice maintains backlog, test evidence, GenAI and contribution records. Do not rewrite original sources. Update the GitHub snapshot as needed, stating date and method; do not treat it as live state. Maintain English throughout, including generated artifacts.

## Working with Superpowers
Read the original skill instructions; do not duplicate their workflows here. `using-superpowers` guides discovery; `brainstorming` supports decisions and approval; `writing-plans` follows approved design only; `verification-before-completion` precedes success claims. For future implementation, confirm availability and read `test-driven-development`, `systematic-debugging`, `executing-plans`, `requesting-code-review`, `using-git-worktrees` and `finishing-a-development-branch` as applicable. Use `subagent-driven-development` only with applicable delegation authorization. The observed inventory is in the audit; temporary paths may disappear. Do not install skills, create worktrees or commit merely because a skill suggests it when the scope does not authorize it. Skill-generated repository artifacts must also be in English.
