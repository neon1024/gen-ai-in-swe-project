# Individual contribution — jose03pedro

Student: jose03pedro (GitHub). Commits are authored locally as `zpt17`.
Snapshot: 2026-11-18, from `origin/main` (merge commit of PR #65) plus open PR #66. Sources: `git log`, GitHub issues and PRs, and the GenAI entries linked below. Every row can be checked against those references.

## Summary

| Item | Contribution |
|---|---|
| Owned slice | Event Authoring (WF-02 Event lifecycle, authoring part): US-14 Create a draft event (#17), US-15 Add and edit activities (#18), US-16 Manage an activity's agenda (#19), US-17 Publish an event (#20), US-18 Edit a published event (#21) |
| Issue status | #17–#20 closed; #21 implemented and merged (PR #59, #60), issue still open pending review |
| Assigned, not started (Final milestone) | US-19 Notify participants of changes (#22), US-33 Make an activity private (#38), US-34 Invite users to a private activity (#39), US-35 Respond to an invitation (#40), US-42 Send an announcement (#48) |
| Commits on `main` | 64 non-merge commits |
| Pull requests authored | #59, #60, #64, #65 (merged); #66 (open) |
| Pull requests merged as integrator | #1, #58, #59, #60, #61, #62, #63, #64 |

## Contribution by area

| Area | What was done | Evidence |
|---|---|---|
| Implemented functionality (backend) | Event Authoring module `com.ubb.genai.events.authoring`: drafts, activities, agenda items, publication, editing of published events with optimistic versioning, fail-closed registration guard for capacity changes, and projection of published events into the lifecycle/discovery model | PR #59, PR #60 (`LifecycleProjection`); about 1,800 lines of main code |
| Implemented functionality (frontend) | Authoring UI: new-draft page, event editor with activities, agenda and publish; API client with cookie CSRF | PR #60, `frontend/src/features/event-authoring/` |
| Integration | Merged the four mid-term PRs (#60–#63) and connected them: session-based identity across auth, lifecycle, organizations and authoring; one SPA security setup (cookie CSRF, JSON 401/403); conflict resolution, including conflict markers committed to `main`; schema/migration alignment; organization-aware draft creation | PR #64 |
| Mid-term demo readiness | Review fixes: demo accounts opt-in only, login session-id rotation, JSON login refusals. UI links between search, lifecycle, editor and report; broken participant screens hidden; demo script `docs/demo/mid-term-demo.md` | PR #65 |
| CI/CD and deployment | GitHub Actions (backend `verify`, frontend lint/test/build; GHCR image publishing on `main`), `scripts/ci.sh`, backend/frontend Dockerfiles, nginx same-origin proxy, `compose.yaml` | PR #66 (open; checks passed on GitHub) |
| Tests written | 17 backend test classes for Event Authoring (118 `@Test` methods: API, application, persistence/concurrency, migrations, contracts); frontend authoring tests (`EventAuthoringPage.test.tsx`); integration tests for login → session → authoring, login refusals, session-id rotation, CSRF rejection and pending accounts (`AuthoringWiringTests`) | `backend/src/test/java/com/ubb/genai/events/`, PR #64, PR #65 |
| Documentation | Event Authoring slice design and plan; [authoring contract](../integration/event-authoring-contract.md) with decisions K-1–K-9; [verification record](../requirements/event-authoring-verification.md); decision D-011; architecture/collaboration baseline commit; README run/test/deploy section; demo script | Commits `docs(events)…`, `docs: establish architecture…`; PR #65, #66 |
| Architecture/design decisions | K-1–K-9 (D-011): cancelled activities read-only; draft activity removal without registration guard; registration guard request and lock order; reserved migration range V2026111010–019; category precedence; frontend host adoption; canonical authoring model; authoring-scoped security; shared Flyway history. **Note:** per D-011, the student delegated these decisions to the agent; K-3 and K-7 bind other owners and remain proposals until they accept | [Decisions index](../decisions/README.md) |
| GenAI-related work | All implementation sessions were GenAI-assisted (Codex desktop, Claude Code). Each session is logged; see the entries below | [GenAI usage log](../genai_usage_log.md) |

## GenAI sessions

| Entry | Date | Tool / model as recorded | Scope |
|---|---|---|---|
| [bec0da07](../genai-usage/entries/20261110T000326Z-issue-17-jose03pedro-bec0da07-0ab2-4a02-94c0-46d9045d3b72.md) | 2026-11-10 | Codex desktop; model not confirmed (student-reported) | Event Authoring #17–#21 implementation |
| [aa1a2bb9](../genai-usage/entries/20261110T125439Z-issue-17-jose03pedro-aa1a2bb9-3589-4f5b-9fe8-715221267e13.md) | 2026-11-10 | Codex desktop; not confirmed | Resumed implementation |
| [05c0cb80](../genai-usage/entries/20261110T141041Z-issue-18-jose03pedro-05c0cb80-580d-4b46-897a-e622a56377d0.md) | 2026-11-10 | Codex desktop; not confirmed | Nested-resource correction |
| [00520e67](../genai-usage/entries/20261110T151415Z-issue-21-jose03pedro-00520e67-f17d-430e-9344-ecb193f7ff78.md) | 2026-11-10 | Claude Code; Claude Opus 5.5 | #21 published editing |
| [7a7089d3](../genai-usage/entries/20261116T225656Z-issue-17-jose03pedro-7a7089d3-f15d-4f8a-9a99-1fd2abdfe29d.md) | 2026-11-16 | Claude Code; model not confirmed (session reported Sonnet 5.5) | Authoring UI and dev identity |
| [016847b9](../genai-usage/entries/20261118T002322Z-issue-17-jose03pedro-016847b9-2bd4-460d-9504-49c870855afb.md) | 2026-11-18 | Claude Code; Claude Opus 5.5 | Integration of PRs #60–#63, demo readiness, CI/CD, this report |

## Human decisions that changed the AI-generated work (session 016847b9)

| Decision by the student | Effect |
|---|---|
| Merge the four open PRs and integrate frontend with backend | Integration branch and PR #64 instead of separate merges |
| Asked whether to implement teammates' missing issues (registrations #30–#35, organization registration #5–#7), the student chose **not** to | Protects teammates' ownership and their individual/No-GenAI evaluation; the demo hides those screens instead (PR #65) |
| Asked to make the app work for the mid-term presentation | Scope limited to review fixes, navigation and a demo script |
| Asked for CI/CD both locally and on GitHub Actions | `scripts/ci.sh` mirrors the workflow; compose runs the published images (PR #66) |

## Pending

- Human review of the code: all GenAI entries are `Pending student review`.
- Individual No-GenAI feature (DEL-005): not yet defined (open question Q-09).
- Final-milestone issues #22, #38–#40, #48.
