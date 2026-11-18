# Requirements Interview — Decision Log

Working record of the requirements-elicitation interview between the requirements owner
(Duarte Lagoela) and GenAI (Claude Opus 5.5, Claude Code, superpowers *brainstorming* skill).
Started 2026-10-12. Each row records the AI proposal, the decision taken, and whether the
human changed the AI's recommendation (evidence for R14 / GenAI declaration).

Requirement sources are the brief's requirements (R1–R16). Validation by real users is recorded in
`06-user-research.md`: hypotheses (H-xx) linked to requirements, confirmed or changed by findings (F-xx).
Earlier rows mentioning `ASSUMPTION` tags predate D-26.

## Decisions

| # | Topic | AI proposal | Decision | Human change |
|---|-------|-------------|----------|--------------|
| D-01 | User research status | Document assumptions first, validate with real interviews later | Accepted. No real interviews yet; requirements tagged `ASSUMPTION` until validated | — |
| D-02 | Actors | Participant + Organizer as given; offered Guest, Org Admin, Platform Admin, External Participant | All six accepted (External Participant later merged, see D-13) | — |
| D-03 | Organization creation | A UBB user requests; Platform Admin approves; requester becomes first Org Admin | Accepted | — |
| D-04 | Organization membership | User requests to join (Org Admin approves) **or** Org Admin invites (user accepts) | Accepted | — |
| D-05 | Role scoping | Per-organization roles (Member / Organizer / Org Admin); user may belong to several orgs; per-event team | Accepted | — |
| D-06 | Authentication | Email + password with email verification; UBB status from email domain | **Replaced by tech constraint:** Supabase Auth provides identity (email + password, confirmed email). Authorization (org roles, event team, Platform Admin) is enforced by the application | Yes |
| D-07 | Event lifecycle | Stored states DRAFT → PUBLISHED → COMPLETED, CANCELLED from DRAFT/PUBLISHED; registration status (NOT_YET_OPEN/OPEN/FULL/CLOSED) and timing (UPCOMING/ONGOING/ENDED) derived from dates and capacity | Accepted. No event approval step (Org Admin approval kept as an assumption to validate) | — |
| D-08 | Location / venues | Venue catalog + clash/capacity warnings | **Rejected.** Free-text location + optional online link. Venue management out of scope | Yes |
| D-09 | Event schedule | Single time slot per event | **Rejected.** Event → one or more **Activities** (e.g. day of talks, coding competition), each with own date/time, location, capacity, registration and attendance; each activity has an optional agenda (time, title, speaker). Participants register per activity so they don't take places in activities they won't attend | Yes |
| D-10 | Overlapping signups | Block signups that overlap an existing confirmed signup | Accepted | — |
| D-11 | Full activity | Waitlist with automatic promotion | Accepted. Promotion skips a waitlisted user who now holds an overlapping confirmed signup | — |
| D-12 | Participant cancellation | Allowed until activity start; freed place goes to waitlist | Accepted | — |
| D-13 | Eligibility / visibility | Per-activity: Open to all / UBB only / Org members only; events always visible | **Changed.** One setting per activity: **Public** (default; visible to all incl. guests; any logged-in user may sign up) or **Private** (visible to and joinable by org members + invited users only; hidden from everyone else). UBB-only level and email-domain verification dropped; External Participant merged into Participant. Rationale: keep configuration minimal for organizers | Yes |
| D-14 | Invitations to private activities | Shareable invite link (resettable) | **Rejected (leak risk).** In-app **inbox** where the invitee accepts or declines, plus an email notification sent by the app. Inviters never see invitee emails. Inbox is reused for org invitations and other notifications. Accepting = signing up (normal capacity/waitlist/overlap rules) — *default, pending confirmation* | Yes |
| D-15 | Finding invitees | Unique username chosen at signup; Organizers/Org Admins search by username or display name | Accepted | — |
| D-16 | Event team roles | Owner / Editor / Helper | **Changed:** "Helper" renamed **Staff**; explicit hierarchy Owner ⊃ Editor ⊃ Staff (higher role has all lower capabilities; Staff gains more only by promotion). AI defaults added: Owner and Editors add Staff; only Owner promotes/removes Editors; Owner can transfer ownership to an Editor; Org Admin has Owner rights on all of the org's events | Yes |
| D-17 | Responsibilities | Simple task list: title, description, assignee(s) from event team, optional due date, To do / Done, optional link to an activity; Editors/Owner create and assign, Staff mark own tasks done | Accepted | — |
| D-18 | Attendance | Personal QR code per confirmed signup scanned by Staff; manual mark-present from searchable signup list as fallback | Accepted. Defaults: no penalty for no-shows; walk-in registration out of scope (assumption to validate) | — |
| D-19 | Notifications | Offered: changes & cancellations, waitlist promotion, organizer announcements, reminder before start | All four accepted. Every notification goes to inbox + email; no per-user notification preferences (assumption to validate) | — |
| D-20 | Search & discovery | Keyword search + date and organization filters as baseline; recommended categories, free-places filter, follow organizations | **Partly rejected.** Only fixed categories (Workshop, Conference, Training, Social, Competition, Other) added. Free-places filter, free tags and following organizations dropped (following kept as assumption to validate) | Yes |
| D-21 | Post-event | Recommended feedback, event report, platform-wide statistics | **Partly rejected.** Feedback (attendees only, 1–5 + optional comment) and event report (per activity: signups, waitlist, cancellations, attendance rate, average rating; CSV export) kept. Platform statistics and attendance certificates dropped ("information for the university" appears only in the brief's general description, not R1–R16) | Yes |
| D-22 | Editing after publication | Free edit with guardrails: time/location changes notify; capacity ≥ confirmed count; capacity increase promotes from waitlist; activities with signups and published events can only be cancelled, not deleted | Accepted | — |
| D-23 | Completion | Event becomes COMPLETED automatically when its last activity ends; 7-day window per activity for attendance corrections and feedback, then read-only | Accepted | — |
| D-24 | Mid-term MVP | Core loop: account + username, org request & approval, membership & roles, create/publish event with activities & agenda, browse/search, sign up / cancel / waitlist | Accepted. Remaining workflows (private activities & inbox, event team & tasks, notifications/email, check-in, feedback & report) target the final delivery | — |
| D-25 | Deliverable format | Markdown in `docs/requirements/`, one file per section, Mermaid diagrams | Accepted | — |
| D-26 | Document style | Requirement sources tagged `ASSUMPTION`; "Rel." column; Supabase named in scope; payments listed as out of scope; change notes and decision references inline | **Changed:** sources are brief requirements (R1–R16) only, validation lives in the user-research hypotheses; "Milestone" column (Mid-term / Final); no technology names; payments never mentioned; no meta commentary in deliverables | Yes |
| D-27 | Traceability status | Implementation, tests and status columns in `07-traceability.md` | **Changed:** live status tracked on GitHub (issues, milestones, pull requests); the Markdown matrix holds only the fixed links FR/BR → UC → US → WF | Yes |
| D-28 | Backlog structure | Workflow parent issues with one sub-issue per use case | **Changed:** sub-issues are user stories. On "do what is industry best practice": INVEST split of large use cases and of parts delivered in a later milestone (50 stories), "As a / I want / so that" format, checklist acceptance criteria, Definition of Done | Yes |
| D-29 | GitHub publication | — | Nothing is created on GitHub (issues, milestones, labels) until the other team members approve the documents | Yes |
| D-30 | AI self-review fixes | Found while mapping stories and running ID cross-checks | UC-01, UC-02, UC-31 added to WF-01 and UC-20 to WF-04 (they belonged to no workflow); WF-04 renamed "Invitations and inbox"; FR-ACC-05 and BR-25 added (granting Platform Admin was in the approved role table without a requirement); BR-07 extended to agenda items; BR-23 extended to removal by an Org Admin; FR-NTF-03/05 audience made explicit (confirmed or waitlisted); deactivating a user cancels their future signups (UC-31, US-11) | Pending review |

## Status

Documents drafted 2026-10-14 (`01`–`08` in this folder). Automated checks passed: every ID defined
and cross-referenced in both directions, every FR covered by a user story, every use case in exactly
one workflow, all 12 Mermaid diagrams parse. Next: review by the requirements owner, then by the
other team members, then creation of GitHub issues.
