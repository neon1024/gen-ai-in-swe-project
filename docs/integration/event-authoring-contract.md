# Event Authoring integration contract

Status: **Accepted by the requesting student for isolated Event Authoring implementation, 2026-11-10**. Section H interpretations and minimum setup were explicitly approved. Cross-owner API/schema/lock agreements and independent student review remain pending; no agreement by Duarte or another owner is invented. Date: 2026-11-10. Owner: jose03pedro. Scope: #17–#21 only. This is a concrete review proposal, not an accepted agreement with Duarte or another contributor. No code, API, table, adapter or test described below exists yet unless explicitly identified as existing. Human Review Status: Pending student review.

## A. Scope, evidence and ownership

[Requirements](../requirements/02-requirements.md), [roles](../requirements/03-actors-and-roles.md), [UC-10–12](../requirements/04-use-cases.md), [US-14–18](../requirements/08-user-stories.md) and [ADR register](../decisions/README.md) remain authoritative. The latest user mission authorizes independently testable work despite incomplete M1/M2, but expressly retains slice-design approval gates.

| Issue | Story | Owned behavior | External completion dependency |
|---|---|---|---|
| [#17](https://github.com/neon1024/gen-ai-in-swe-project/issues/17) | US-14; FR-EVT-01; BR-03 | Create Draft, assign actual creator as Owner, restricted authoring reads | Real identity/account eligibility and org membership/role adapters (#3–10) |
| [#18](https://github.com/neon1024/gen-ai-in-swe-project/issues/18) | US-15; FR-EVT-02; BR-07/08 | Activity add/edit/remove, public Mid-term visibility, registration-window metadata | Real access control and signup guard before destructive operations |
| [#19](https://github.com/neon1024/gen-ai-in-swe-project/issues/19) | US-16; FR-EVT-03; BR-07 | Agenda create/edit/remove/read and interval integrity | Real access control |
| [#20](https://github.com/neon1024/gen-ai-in-swe-project/issues/20) | US-17; FR-EVT-04; BR-01/02 | Draft → Published, complete readiness errors and persisted publication metadata | Real discovery consumer (#28) and registration consumer for full acceptance |
| [#21](https://github.com/neon1024/gen-ai-in-swe-project/issues/21) | US-18; FR-EVT-05; BR-12/14 | Published event/activity/agenda editing, guarded capacity/removal operations | Real signup/FIFO implementation (#31/#33) and shared lock/transaction protocol |

Excluded operations: authentication/onboarding, organization/membership management, event-team administration, private access/invitations (#38–40), notifications (#22/#48), cancellation (#23/#24), draft-event deletion (#25), automatic completion (#26), discovery UI/search, signup/FIFO algorithms, attendance/reports/feedback. No endpoint allows arbitrary lifecycle state/Owner changes.

### Inspected baseline and remote work

Main is e021b231a0018df82cffb59ac63e8f98688abf91 and contains only Spring bootstrap, GET / → Hello, a context test and preparation documentation. Public GitHub inspection found no open PRs, all five owned issues open with criteria unchecked, and no committed shared auth/org/event/signup contracts. The remote develop/frontend branch at 269375598446b545026ac100e6a790dcddb9fcaa is a Next.js 16.4.0 starter authored by lucianvaleanu, not the approved Vite foundation. Do not overwrite, merge or convert it silently. Unpublished work remains unknown.

## B. Proposed domain model

Use com.ubb.genai.events.authoring with api/application/domain/persistence/ports as needed, without separate layers that merely forward calls. EventAuthoringService coordinates real domain rules and repositories. Avoid a second global Event model; this is the proposed canonical authoring-owned aggregate to be reconciled with lifecycle consumers.

| Concept | Proposed fields / types | Invariants and references |
|---|---|---|
| Event | id UUID; organizationRef opaque String; ownerSubject opaque String (current Owner); title/description String; category enum; state enum; createdAt Instant; publishedAt optional Instant; version long | Required nonblank title/description/category/org; initial Owner from trusted server identity; IDs/org/Owner immutable through authoring commands; current Owner may later change only through the real ownership-transfer workflow; no invented external UUID assumption |
| Activity | id UUID; eventId UUID; title/description String; start/end OffsetDateTime at API, persisted as instants with input offsets retained; location/onlineLink optional individually; capacity int; registrationOpen/Close optional offset timestamps; visibility enum; cancelled boolean; createdAt Instant | Correct parent; end strictly after start; capacity >=1; at least location or online link under the proposed complete-write policy; registration closing <= start |
| AgendaItem | id UUID; activityId UUID; time offset timestamp/instant; title String; speaker optional String | Required time/title; parent exists; time falls within activity interval; order by time then stable ID |

Event states: Draft, Published, Completed, Cancelled. Only Draft → Published is implemented here. Duarte owns cancellation, completion and draft-event deletion. Activity cancellation is lifecycle-owned; the stored cancellation flag is represented/read but not exposed as an authoring command. Registration status (Not yet open/Open/Full/Closed) and timing remain derived, not additional persisted state. No private toggle is implemented: UC-10 explicitly assigns private visibility to Final; preserve an enum value for future integration while rejecting private writes in this Mid-term slice.

Categories remain Workshop, Conference, Training, Social, Competition, Other as approved in US-25; serialization names and enum mapping are documented with the API rather than adding categories. No user/email/profile, organization or signup tables are duplicated. Org/user references remain opaque until their real contracts are agreed.

### Authorization

Read Draft only for the actual event team (including Staff) and that organization's Org Admins. No additional blanket prohibition on authoring for an inactive organization is introduced by this contract: BR-24’s discovery/signup deactivation effects remain the respective owners’ integration responsibility. Create only for an eligible Organizer or Org Admin of the selected organization. Creator becomes the current Owner atomically in Event.ownerSubject, so creation does not require inventing an external team record. That field is authoritative for current Owner privileges; external team lookup supplies Editor/Staff roles, not a conflicting Owner claim. The real #45 ownership-transfer workflow (All16) must update this current-Owner field atomically and return the previous Owner to Editor through its own team contract; creator identity must never grant permanent Owner rights after transfer. Current organization membership/account eligibility still comes from authoritative adapters. Edit/add/remove/publish require Owner or Editor, or that organization's Org Admin with existing owner-equivalent rights. Staff alone cannot publish/edit; Platform Admin and ordinary Organizer have no automatic rights on someone else's event. Completed/Cancelled events are read-only for authoring. Resolve nested activity/agenda resources through their actual parent before authorizing; never authorize an unrelated event ID supplied in the body.

## C. Persistence and concurrency proposal

Proposed isolated PostgreSQL schema event_authoring with tables event, activity and agenda_item. All aggregate IDs use UUID; external identity/organization references use text, with no invented foreign keys to absent tables. activity.event_id and agenda_item.activity_id are real internal foreign keys. Required data uses NOT NULL/check constraints; end > start and capacity >=1 are constrained. Parent/date listing indexes are added only for actual authoring reads. The Owner is an event field; no event-team administration table is introduced. Real team/Org Admin lookup remains external.

Reserve one initial Flyway migration version only after a final collision check: backend/src/main/resources/db/migration/V2026111001__event_authoring_initial.sql. Schema/version names are proposals, not shared reservations already accepted by other students. Retain one common Flyway mechanism; do not introduce a conflicting migration runner or destructive Hibernate schema updates. Discuss exact table adoption with Duarte before his lifecycle migration uses it.

Local authoring-only writes are transactional and lock the parent Event before nested rows. This local order is not an accepted cross-module lock order. A version precondition prevents stale authoring submissions. Publish and capacity changes serialize with other aggregate changes. For signup-sensitive operations, merely reading a count then saving is unsafe: the real registration adapter must participate in the same transaction and approved activity/participant lock protocol used by signup/cancellation/promotion. The exact cross-module locking contract requires the registration owner; test doubles verify authoring call semantics only. Missing/failed registration adapter must refuse affected operations, not return zero signups or silently skip promotion. No parallel signup/FIFO implementation, fake production count adapter or asynchronous promotion claim is permitted.

## D. Proposed REST/JSON API

Prefix /api/event-authoring, deliberately namespaced until team reconciliation. These are proposed authoring routes, not existing global API conventions. Mutation DTOs never accept actor, Owner, event state or cancellation. Organization is selected only on create and always checked server-side. Local IDs and versions come from the server. Successful responses return an immutable EventView including activity/agenda data and current version. EventView contains event id, organizationRef, title/description/category, state, createdAt/publishedAt, version, nested public activity/agenda fields and server-derived capabilities {canEdit, canPublish}. It omits ownerSubject, actor/account data and emails; the UI uses capabilities for read-only Staff affordances without treating them as server authorization. POST/PUT mutations of an existing aggregate require expectedVersion in the command DTO; DELETE uses the expectedVersion query parameter and no request body. Mismatch is 409 with no writes. Every successful mutation returns the updated EventView (201 for creates, otherwise 200), including the next aggregate version; deletion never leaves the client guessing the version. Public discovery is not this authoring read API.

| Method / URL | Purpose / requirement | Authorization | Request / success |
|---|---|---|---|
| POST /events | Create Draft; FR-EVT-01 | eligible Organizer/Org Admin of selected org | CreateEvent(title, description, category, organizationRef); 201 EventView + Location |
| GET /events/{eventId} | Load authoring detail | actual team or same-org Org Admin | 200 EventView; no unrelated draft data |
| PUT /events/{eventId} | Edit title/description/category; FR-EVT-05 | Owner/Editor or same-org Org Admin | EventDetails(expectedVersion, title, description, category); 200 EventView |
| POST /events/{eventId}/activities | Add activity; FR-EVT-02 | same edit policy | ActivityWrite plus expectedVersion; 201 EventView |
| PUT /events/{eventId}/activities/{activityId} | Replace activity details; FR-EVT-02/05 | same edit policy, validated parent | ActivityWrite plus expectedVersion; 200 EventView |
| DELETE /events/{eventId}/activities/{activityId} | Remove unsigned activity only; FR-EVT-02, BR-14 | same edit policy and real signup guard | expectedVersion query parameter; 200 updated EventView; no cancellation fallback |
| POST /events/{eventId}/activities/{activityId}/agenda | Add agenda item; FR-EVT-03 | same edit policy | AgendaWrite(expectedVersion, time, title, speaker?); 201 EventView |
| PUT /events/{eventId}/activities/{activityId}/agenda/{itemId} | Replace agenda item; FR-EVT-03 | same edit policy and nested parent | AgendaWrite; 200 EventView |
| DELETE /events/{eventId}/activities/{activityId}/agenda/{itemId} | Remove agenda item; FR-EVT-03 | same edit policy and nested parent | expectedVersion query parameter; 200 updated EventView |
| POST /events/{eventId}/publish | Publish Draft; FR-EVT-04 | Owner/Editor or same-org Org Admin; not Staff alone | VersionCommand(expectedVersion); 200 EventView or complete readiness problems |

Full URLs include the prefix; no route for event deletion/cancellation/completion or team administration is introduced. Returning EventView after writes supports simple feature UI refresh without a speculative state framework.

Example proposed create request:

```json
{"title":"Software workshop","description":"Hands-on session","category":"Workshop","organizationRef":"opaque-org-reference"}
```

Example proposed activity fields (expectedVersion is required on the command):

```json
{"expectedVersion":0,"title":"Workshop session","description":"Practical work","start":"2026-12-01T10:00:00Z","end":"2026-12-01T12:00:00Z","location":"Room A","onlineLink":null,"capacity":25,"registrationOpen":null,"registrationClose":null,"visibility":"Public"}
```

Proposed error envelope: {code, message, violations:[{path, code, message}]}, without stack traces, emails or private resource details. Codes: 400 for malformed/invalid input, 401 without authenticated eligible identity, 403 for denied known action, 404 for missing or concealed resources, 409 for state/version/signup conflicts, 422 for accumulated publication readiness problems, 503 for an unavailable mandatory external adapter. Concretize concealed-resource behavior consistently in tests before implementation; do not leak draft existence through detailed errors. CSRF failures remain the agreed Spring Security HTTP behavior and are tested distinctly. Exact upstream security/error conventions remain coordination points.

## E. Frontend integration proposal

Own frontend/src/features/event-authoring/ only: feature API client/types, event-details form, activity form/list, agenda form/list, publication feedback and an authoring page. Proposed host routes: /authoring/events/new and /authoring/events/:eventId; no public discovery or login UI. Export a route/component integration surface rather than changing another student's navigation silently.

The host supplies authenticated-user/org choices and session/CSRF behavior; no frontend actor ID proves identity. The feature client uses same-origin credentials and a caller-supplied CSRF token/header contract, with loading/error/success/empty states and accessible labels. Read responses, not local assumptions, determine resulting state/version. No fake successful API server is production wiring.

Because main has no frontend foundation and develop/frontend uses Next.js, the requesting student explicitly approved this minimum isolated setup: preserve Vite per accepted ADR, introduce only a Vite/React/TypeScript feature test/development harness and locked dependencies needed by this share, and do not merge/overwrite the other branch. This setup does not claim to complete all M1 or decide the colleague's future frontend migration. A host-independent feature-only package is an alternative if the team does not authorize root frontend configuration.

## F. Required external ports and adapter status

Signatures below are proposed contracts, not existing services. Real adapters are absent; no default permissive production implementation is allowed.

| Port / proposed capability | Inputs / outputs | Required semantics / owner | Actual status |
|---|---|---|---|
| AuthoringIdentityPort.currentActor | no client actor input → authenticated subject + account eligibility | trusted existing server session principal and authoritative eligibility; accounts owner | Absent; test-only actor fixtures permitted |
| OrganizationAccessPort.forActor | subject + organizationRef → active membership/org role; eligible org choices supplied by host | authoritative current organization/membership state; organization owner | Absent; no copied member tables |
| EventTeamAccessPort.roleFor | subject + eventId → Editor/Staff role or none | real event-team policy; team owner; Event.ownerSubject is authoritative for current Owner, with membership still authoritative externally | Absent; Editor/Staff fixtures only in tests, no role-management UI |
| RegistrationAuthoringPort.guardMutation | event/activity IDs + expected version + proposed activity mutation in active transaction → guarded mutation result | count confirmed, preserve signups, deny illegal capacity/removal, perform eligible capacity-increase promotion atomically only before activity start; signup/waitlist owner | Absent; affected production commands fail closed; controlled doubles do not verify FIFO |
| PublishedEventsReadContract | committed projection: event/activity IDs, organizationRef, event title/description, category, public/private visibility, state, activity cancellation, schedule, location/link, capacity and effective registration windows | discovery owner queries committed state, resolves searchable organization name through the organization owner using organizationRef, and combines authoritative current organization activation/audience policy (BR-18/24); hide deactivated-org events and close signups when that owner’s feature exists; no Owner/account/email data or separate search index/broker | Proposed read contract only; real search consumer absent |

The real registration port must use the same database transaction and a jointly agreed global lock order, with rollback on failure and no duplicate promotion on retry. In particular, local Event-first locking must not be combined blindly with a participant-first signup flow. Until the registration owner accepts a compatible protocol, affected production commands remain unavailable. Promotion never runs after activity start; increasing capacity after start does not waive that boundary. Preserving any historical signup row for deletion is a policy proposal in H, not an existing interpretation already accepted by the team. Any optional idempotency identifier must be specified with the real owner, not invented as proof of delivery. Missing ports produce an explicit dependency-unavailable outcome. Tests may assert mutations fail without them; a test fixture is not a real integration.

## G. Shared-file change register (requesting-student minimum setup approved)

| Proposed shared change | Reason / boundary | Owner/coordination status |
|---|---|---|
| backend/pom.xml | Approved JPA/PostgreSQL/Flyway/security and test dependencies, compatible managed versions | Minimum isolated feature setup approved by requesting student; not yet changed |
| backend resource configuration / Flyway location | isolated test database and selected schema; no destructive updates/credentials | Coordinate with foundation and lifecycle owners; no change yet |
| Feature-scoped security wiring | session/CSRF protection for authoring, never parallel login/signup or permissive missing adapter | Shared foundation boundary; minimum scoped wiring approved for this isolated slice, cross-owner integration pending |
| frontend package/tool config and host entry | test/build real feature with approved React/Vite tools | Known Next.js branch overlap; minimum isolated Vite setup approved by requesting student, no files changed |
| Project-local test infrastructure | independent PostgreSQL containers; never use unrelated opsrelay database on localhost | Docker exists; project infrastructure absent; no container started |

Do not add global mail/CI/team/search setup that this share does not need. All shared changes must be listed with actual files/commits once implemented.

## H. Decisions, working assumptions and approval record

Confirmed: five stories/criteria, ADR direction, ownership, no shared event contract in inspected branches, public-only Mid-term activities, strict end > start, preserved signups on a time-change overlap (UC-12), and no implementation of other owners' transitions.

Technical choices approved for isolated implementation by the requesting student: namespace/API/schema/ID conventions, mutable authoring fields with stable org/Owner, DTO version preconditions and transaction protocol, feature wiring and minimal shared setup. They are not agreements already made with Duarte.

Business interpretations explicitly approved by the requesting student for this slice (not retroactively part of the original source):

1. Complete activity writes rather than partial draft rows: all described authoring fields required except individually optional location/link (at least one required), registration windows and speaker; publication still revalidates every activity and reports all problems.
2. Agenda time may equal start or end (inclusive interval); changing an activity interval that strands an agenda item is refused atomically without shifting/deleting agenda items.
3. Effective registration opens at max(publication time, explicit later opening); a newly added Published activity is not available before its own creation/commit. Defaults follow schedule changes. Explicit close after start is invalid; explicitly reversed/equal configured windows are refused. Publication after a registration window ended does not invent a future-time requirement; registration remains closed.
4. Any signup history blocks activity removal; removing the last activity of a Published event is refused to retain a publishable aggregate. The latter is an additional policy proposal, not a rule already explicit in BR-02.

Named timezone policy remains Q-05; proposed UI explicitly labels UTC inputs and sends offset-bearing RFC 3339 timestamps, never silently applies the host timezone. Retain submitted offset metadata without imposing a new required user-facing field. External ID formats, foreign keys and exact shared lock ordering remain unagreed integration details; do not pretend local choices are cross-team acceptance.

## I. Verification and implementation status

All five stories: **Not Started — executable-plan review pending**. The requesting student approved this isolated slice design; cross-module integration approvals remain pending. No production code, migration or frontend scaffold created. No backend/frontend/DB/browser tests executed in this investigation. Documentation baseline checks pass; actual tool/environment availability is recorded in the session log. Authentication, org/team lookup, discovery and signup/FIFO are **Pending integration**, not working dependencies.

After approval, produce one writing-plans implementation plan, then use a fresh implementer per small task followed by independent spec review and code-quality review in that order. Use TDD, PostgreSQL integration where runnable, real UI/API tests where real adapters exist, and explicitly separate fixture verification from real E2E. Every acceptance criterion maps to a named test and a real/double/pending status. No issue closure/merge without the applicable human steps.
