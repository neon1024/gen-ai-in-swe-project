# Event Authoring integration contract

Status: **Accepted by the requesting student for isolated Event Authoring implementation, 2026-11-10**. Section H interpretations and minimum setup were explicitly approved. Cross-owner API/schema/lock agreements and independent student review remain pending; no agreement by Duarte or another owner is invented. Date: 2026-11-10. Owner: jose03pedro. Scope: #17–#21 only. This is a concrete review proposal, not an accepted agreement with Duarte or another contributor. EA-01 setup, migration and infrastructure tests now exist as recorded in section G; authoring domain/API/UI behavior and external adapters remain unimplemented unless explicitly identified below. Human Review Status: Pending student review.

**Latest user execution boundary:** finish publication #20 backend now, defer #21 until a later prompt, and stop before later UI/integration tasks. The approved future contract remains unchanged; current availability and preserved work are recorded below.

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

### Initial inspection snapshot (superseded by the later PR #58 evidence below)

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

EA-01 introduces PostgreSQL schema event_authoring and tables event, activity and agenda_item through the initial migration. Logical domain/JPA/API integration is still forthcoming. All aggregate IDs use UUID; external identity/organization references use text, with no invented foreign keys to absent tables. activity.event_id and agenda_item.activity_id are real internal foreign keys. Required data uses NOT NULL/check constraints; end > start and capacity >=1 are constrained. Parent/date listing indexes are added only for actual authoring reads. The Owner is an event field; no event-team administration table is introduced. Real team/Org Admin lookup remains external.

EA-01 checked that no migrations existed and added the initial authoring migration, renamed on 2026-11-10 (commit 8649923, identical content) to backend/src/main/resources/db/migration/V2026111010__event_authoring_initial.sql. Authoring reserves Flyway versions V2026111010–V2026111019 (decision K-4). Schema/version names are proposals, not shared reservations already accepted by other students. Retain one common Flyway mechanism; do not introduce a conflicting migration runner or destructive Hibernate schema updates. Discuss exact table adoption with Duarte before his lifecycle migration uses it.

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
| RegistrationAuthoringPort.withLockedActivity | event/activity IDs + expected version + proposed activity mutation in active transaction → guarded mutation result | count confirmed, preserve signups, deny illegal capacity/removal, perform eligible capacity-increase promotion atomically only before activity start; signup/waitlist owner | Interface and unavailable default implemented in EA-03; real adapter absent; affected production commands fail closed; controlled doubles do not verify FIFO |
| PublishedEventsReadContract | committed projection: event/activity IDs, organizationRef, event title/description, category, public/private visibility, state, activity cancellation, schedule, location/link, capacity and effective registration windows | discovery owner queries committed state, resolves searchable organization name through the organization owner using organizationRef, and combines authoritative current organization activation/audience policy (BR-18/24); hide deactivated-org events and close signups when that owner’s feature exists; no Owner/account/email data or separate search index/broker | Proposed read contract only; authoring projection is not connected to the provisional discovery consumer in PR #58 |

The real registration port must use the same database transaction and a jointly agreed global lock order, with rollback on failure and no duplicate promotion on retry. In particular, local Event-first locking must not be combined blindly with a participant-first signup flow. Until the registration owner accepts a compatible protocol, affected production commands remain unavailable. Promotion never runs after activity start; increasing capacity after start does not waive that boundary. Preserving any historical signup row for deletion is a policy proposal in H, not an existing interpretation already accepted by the team. Any optional idempotency identifier must be specified with the real owner, not invented as proof of delivery. Missing ports produce an explicit dependency-unavailable outcome. Tests may assert mutations fail without them; a test fixture is not a real integration.

## G. Shared-file change register (requesting-student minimum setup approved)

| Proposed shared change | Reason / boundary | Owner/coordination status |
|---|---|---|
| backend/pom.xml | Approved JPA/PostgreSQL/Flyway/security and test dependencies, compatible managed versions | Implemented in EA-01 commits f41c52a and f967285; specification review passed, quality review passed at 2b8163f |
| backend resource configuration / Flyway location | isolated test database and selected schema; no destructive updates/credentials | EA-01 added migration/resource; EA-02 imports it for runtime; integration coordination remains pending |
| Feature-scoped security wiring | session/CSRF protection for authoring, never parallel login/signup or permissive missing adapter | Implemented in EA-02 with scoped authoring protection and baseline fallback; cross-owner integration remains pending |
| frontend package/tool config and host entry | test/build real feature with approved React/Vite tools | EA-01 created isolated Vite host/toolchain on this branch; develop/frontend untouched |
| Project-local test infrastructure | independent PostgreSQL containers; never use unrelated opsrelay database on localhost | EA-01 used disposable Testcontainers postgres:17.6-alpine on a dynamic port; no unrelated database used |

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

#17 backend work is **Locally verified under explicit fixtures in completed EA-02 at 7263e0c**; its UI and real upstream integration remain pending. #18/#19 backend work is **Locally verified under fixtures in completed EA-03 at 4ecc06f**; #20 backend publication is **Locally verified and independently reviewed at db799b9**. #21 Published editing backend is **Locally verified and independently reviewed at 4cce076** on feat/issue-21-jose03pedro-edit-published-event; UI and real registration integration remain pending. EA-01 minimum setup is **Complete at 2b8163f — specification and quality reviews passed**. The requesting student approved the slice and written plan. Commit f41c52a adds the physical migration/test infrastructure and minimum frontend host, not completed authoring behavior. Real database migration and draft API tests and an RTL shell test were executed; draft behavior is verified under fixtures, while browser and real upstream integration remain pending. Cross-module integration approvals remain pending. Documentation checks pass; test evidence and review findings are recorded below and in the session log. Authentication, org/team lookup, discovery and signup/FIFO are **Pending integration**, not working dependencies.

After approval, produce one writing-plans implementation plan, then use a fresh implementer per small task followed by independent spec review and code-quality review in that order. Use TDD, PostgreSQL integration where runnable, real UI/API tests where real adapters exist, and explicitly separate fixture verification from real E2E. Every acceptance criterion maps to a named test and a real/double/pending status. No issue closure/merge without the applicable human steps.

## EA-01 implementation evidence (not story completion)

Initial setup commit: f41c52a. JDK21 and Boot4.1.1/Maven3.9.16/Jupiter6.0.3 preserved. Resolved managed dependencies include Hibernate7.4.5.Final, Security7.1.1, Flyway12.4.0, Testcontainers2.0.5 and PostgreSQL JDBC42.7.13. springdoc is deliberately deferred until real endpoints in EA-02. Frontend locked versions: React19.3.1, React Router7.18.5, Vite8.3.7, Vitest5.1.0, Tailwind4.3.5 and TypeScript7.0.2; Node engines are aligned to ^22.22.2 || ^24.15.0 || >=26.0.0 in f967285.

Actual shared/setup files: root .gitignore; backend/pom.xml; existing BackendApplicationTests.java test wiring; authoring support/PostgresTestConfiguration.java; persistence/AuthoringMigrationTests.java; initial SQL migration; main/resources/event-authoring.properties; frontend/package.json and package-lock.json, tsconfig.json, vite.config.ts, index.html, src/main.tsx, src/App.tsx, src/App.test.tsx, src/index.css and src/test/setup.ts. These changes are feature test/build prerequisites, not another owner's account/org implementation.

At EA-01 the authoring resource was opt-in through test configuration; EA-02 now imports it at runtime: Hibernate validate (not destructive updates), open-in-view=false and Flyway history/default schema event_authoring. JPA entity mappings must explicitly use event_authoring; connection default is public. EA-02 implements the production import and scoped fail-closed security as recorded below. The initial setup itself did not implement an authoring endpoint or authentication workflow.

Observed implementer RED: real PostgreSQL table inventory was empty (1 failure, 0 errors); frontend lacked accessible authoring main. Implementer GREEN and inspected Surefire reports: 20 migration/constraint tests plus contextLoads, no errors/failures/skips; frontend 1 test, typecheck/build passed. Fresh spec review found whitespace-only SQL metadata, timestamp/offset pair and Node-engine gaps. Same-worker correction f967285 added regression tests and fixed these gaps. Re-review found EA-01 specification compliant. Coordinator rerun on 2026-11-10 passed 34 backend tests (33 migration plus context), 1 frontend test, typecheck and build. Independent quality review passed with no critical/important findings; the minor TypeScript/Vitest globals alignment was resolved in 2b8163f, independently re-reviewed and verified by repeated coordinator frontend checks. EA-01 is complete.

Actual backend invocation:

```bash
rtk proxy env JAVA_HOME=/usr/lib/jvm/java-21-openjdk MAVEN_USER_HOME=/home/zpt17/ERASMUS/GenAI/gen-ai-in-swe-project/.cache/event-authoring/maven-user bash backend/mvnw -B -ntp -f backend/pom.xml -Dmaven.repo.local=/home/zpt17/ERASMUS/GenAI/gen-ai-in-swe-project/.cache/event-authoring/maven-repository test
```

For another clone, replace the two absolute cache paths with paths inside that clone's ignored .cache directory. Frontend scripts now actually exist:

```bash
rtk proxy npm --prefix frontend run test -- --run
rtk proxy npm --prefix frontend run typecheck
rtk proxy npm --prefix frontend run build
```

All external identity/org/team/signup/discovery adapter and real production E2E statuses remain pending. Docker containers belong only to isolated test runs. The independent review evidence is AI-assisted verification; Human Review Status remains Pending student review.

## J. Resumption coordination snapshot — PR #58 (2026-11-10)

Read-only inspection found [Duarte's open PR #58](https://github.com/neon1024/gen-ai-in-swe-project/pull/58), head c90315eb7393e46dabb81c02db8a3c88d7f70cb9. This supersedes the initial no-open-PR snapshot; main remains e021b23. No remote work was merged, rewritten or adopted. The [lifecycle contract at that head](https://github.com/neon1024/gen-ai-in-swe-project/blob/c90315eb7393e46dabb81c02db8a3c88d7f70cb9/docs/integration/event-lifecycle-contract.md) calls its Event/Activity/Agenda model provisional and assigns replacement to the authoring model during reconciliation. Isolated authoring continues under the existing student-approved contract; these differences are coordination items, not silently approved changes.

| Surface | Observed difference | Gate before combined integration |
|---|---|---|
| Flyway | Resolved on the authoring side by K-4: authoring now uses V2026111010 (reserved range V2026111010–V2026111019); lifecycle uses V2026111001/V2026111002 under db/migration/eventlifecycle | Agree unique versions/locations and migration-history handling for already-applied databases; separate tables/schemas alone do not remove duplicate-version discovery |
| Aggregate and lifecycle | Separate lifecycle_event/activity/agenda tables, UUID external references, transition timestamps, activity state, incomplete activity rows and deletion cascades; authoring has schema-qualified tables, opaque external refs, current Owner and retained input offsets | Agree a single persisted aggregate, field mapping, history/deletion and read-only transitions; lifecycle's current repository does not read authoring-created events |
| Identity and authority | Lifecycle uses provisional X-User-Id and team Owner lookup; authoring requires trusted eligible identity, authoritative membership, current event Owner and CSRF | Replace provisional identity through the real accounts contract; reconcile IDs/Owner/transfer without weakening authoring checks |
| Discovery/windows | PR #58 implements discovery against lifecycle tables; authoring publication is not consumed there | Reconcile committed authoring projection, effective windows, organization names/activation and visibility, then test actual authoring publication in discovery |
| HTTP/concurrency | Different DTO/error envelopes, lifecycle mutation version handling and response shapes; no agreed signup lock protocol | Agree cross-module edit/cancel/complete/delete concurrency and client/error composition; zero-count stubs do not satisfy the registration guard |
| Shared setup | Both branches change Maven/resources and root Vite host/toolchain | Reconcile approved dependencies, security chains, test resources and host route registration in a reviewed integration change |

No combined Flyway/build/browser run was performed against PR #58. No independent student approval or cross-owner agreement is inferred. The requesting student must review the final integration change with affected owners; this task continues only its owned implementation and explicit safe seams.

## EA-02 implementation evidence (partial #17, not full story delivery)

Commits 05e70e3, be7414a and 7263e0c implement createDraft/getEvent, POST /api/event-authoring/events (201 EventView plus Location) and authorized GET /events/{id}. Production conditional ports fail closed; eligible identity/org/team fixtures exist only in test sources. Authoritative current Owner and same-org membership checks, concealed resources and redacted views are covered. Strict create text fields reject coercion and embedded NUL before adapter lookup/persistence. createdAt is normalized once to PostgreSQL microsecond precision; POST/GET stability is verified with a sub-microsecond clock.

Shared changes: backend/pom.xml pins API-only springdoc3.1.1, backend/src/main/resources/application.properties imports event-authoring.properties, and META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports registers deferred authoring defaults. JPA explicitly targets event_authoring; @Version/transaction rollback are exercised. The scoped authoring security chain keeps session/CSRF protection and excludes Basic authentication; a fallback preserves the prior Boot-managed security behavior for unrelated routes. No authentication provider or onboarding workflow was implemented.

Independent specification review corrected default-bean ordering and JSON scalar coercion; quality review corrected NUL input handling, generated201 response/header, timestamp precision and keyed authorization fixtures. Both re-reviews passed. Coordinator execution at 7263e0c: 95 backend tests, 0 failures/errors/skips, BUILD SUCCESS. Exact Maven/cache command remains above; ignored log .cache/event-authoring/coordinator-ea02-quality.log. Nonfatal framework development-security, schema-exists, Mockito-agent and OpenAPI endpoint warnings remain. Direct persistence/version checks were supplemental after the original HTTP RED; deliberate @Version removal demonstrated regression sensitivity, not retroactive original TDD.

This does not verify account onboarding, real eligibility/membership/team integration, frontend flow, PR58 discovery or student implementation review. EA-03 now consumes the reviewed draft aggregate, ports and persistence foundation.

## EA-03 implementation evidence (partial #18/#19, not full story delivery)

Activity commit a9e7701 and agenda commit aa6fd30 implement the approved nested API inventory, typed ActivityView/AgendaView, real JPA mappings and complete-write temporal rules. Corrections 14dbf39 and 4ecc06f respectively resolve nested resources under the locked Event before authorization/state/version checks and read GET aggregates under a read-only REPEATABLE_READ snapshot. No migrations or other owners' modules changed.

All existing-child commands hold the Event-first local lock and return the persisted incremented root version. Real PostgreSQL tests cover stale/concurrent child writes, agenda cascade removal and rollback of root/activity/agenda after a guard failure. Registration removal uses the explicit guard port; its default is unavailable, and fixtures supply snapshots/call probes only. Real signup/FIFO/cross-module lock integration remains pending.

Offset-bearing timestamps retain their submitted offsets. Instants truncate to PostgreSQL microsecond precision before storage, with submitted and canonical activity/window invariants checked; an interval that collapses at that precision returns safe 400. Agenda endpoints remain inclusive. Schedule changes that strand agenda or explicit registration close are rejected atomically. NUL/type/RFC3339/version checks and generated OpenAPI schemas are covered.

The nested-order RED was recovered after the usage interruption rather than repeated: 1 expected assertion failure, 0 errors (404 expected,409 actual). Same-worker correction passed the existing regression. Independent Terra specification re-review approved 14dbf39. Quality review found a multi-statement read snapshot defect; a deterministic test-only Hibernate barrier reproduced old version/new children with 1 assertion failure, 0 errors. REPEATABLE_READ resolved it; quality re-review approved 4ecc06f with no remaining findings. Coordinator full Maven execution at 4ecc06f passed 123 tests, 0 failures/errors/skips; ignored log .cache/event-authoring/coordinator-ea03-snapshot.log. Existing Maven/JDK/cache command remains above.

These are backend and real-storage checks under explicit identity/org/team/signup fixtures. UI, real adapters, discovery reconciliation and student implementation review remain pending. Per-issue milestone refs retain a9e7701 and aa6fd30 while the original authoring branch remains current.

## Publication #20 reviewed handoff and #21 deferral (2026-11-10)

Publication commits 5d95472 and db799b9 implement POST /api/event-authoring/events/{id}/publish, complete accumulated readiness422 problems, strict version/authorization/state checks, stable publication timestamp and atomic PostgreSQL root-version/state persistence. Mid-term visibility is revalidated as Public; independent registration-open/close problems are both retained. Late publication is allowed and already-ended windows remain closed.

PublishedEventsReadContract.publishedEvents() is an internal safe projection. Its read-only REQUIRES_NEW plus REPEATABLE_READ transaction excludes caller/unrelated uncommitted publication and returns coherent root/activity data. Real discovery, organization activation/name/audience and registration consumers are not connected. No colleague module, migration, role or identity contract was integrated.

Historical (superseded by the #21 section below): the user deferred #21 before independent review of its mixed work. That verified 164-test snapshot is retained only on local branch feat/issue-21-jose03pedro-deferred at 437cd5f99de83af0e4b3c0a2fa0aabd730261543. Current publication-only sources exclude EventDetails/metadata PUT, capacity/promotion implementation, CAPACITY_CHANGE and #21 test fixtures. The archive also contains reused publication corrections; preserve newer current fixes when later reconciling #21.

Historical, removed in ea1361a: temporary availability while #21 was deferred: Published authoring child mutations return 409 and EventView capabilities canEdit/canPublish are false. Published reads/projection and Draft editing remain available. Known Staff actions remain 403; foreign nested resources remain 404 before state/version checks. This is a temporary availability boundary, not a permanent change to the approved role or lifecycle rules. Remove it only when guarded Published editing is resumed, verified and reviewed. The future PUT event-details contract in section D is not an implemented current endpoint.

Independent Terra specification and quality reviews approved the publication-only scope. Quality found no critical/important issues; its nonblocking observation is that the internal projection loads full aggregates including unused agenda, so batch projection can be considered with the real discovery consumer. No scale or real-discovery performance claim is made.

Coordinator Maven verify at db799b9 passed 140 tests, 0 failures/errors/skips and executable JAR packaging. Production classes were inspected: publication present; deferred metadata/test fixtures absent. Exact Maven/JDK/cache invocation remains above with goal verify; output .cache/event-authoring/coordinator-issue20-verify.log. Source scope scan and git diff --check passed. Real UI/discovery/registration acceptance and student review remain pending; no GitHub issue was closed or merged.

## Issue #21 Published editing — reviewed backend (2026-11-10)

Branch feat/issue-21-jose03pedro-edit-published-event, stacked on 76f197b. Commits ea1361a, 9798e57 and 4cce076 reconcile only the #21 parts of local WIP 437cd5f. The reviewed Public-only readiness and db799b9 publication hardening are preserved; the WIP's visibility revert and its PublicationTests edits were not carried over.

Implemented: PUT /api/event-authoring/events/{eventId} (EventDetails: expectedVersion, title, description, category; strict deserialization and unknown-field rejection), Published edits/additions of activities and agenda, removal of the temporary 409 gate, and capabilities canEdit=true for Draft or Published (canPublish stays Draft-only). Org, current Owner, state and timestamps stay immutable. The last activity of a Published event cannot be removed (409). Completed/Cancelled remain read-only; Staff 403; foreign nested resources 404 before state/version checks.

Registration seam: on a Published event a capacity change calls RegistrationAuthoringPort.withLockedActivity with MutationKind CAPACITY_CHANGE. Capacity below confirmedCount returns 409 without writes. An increase requests promoteEligibleWaitlist inside the same transaction only when the clock, read after the guard locks, is strictly before the updated start. Correction against the WIP: Draft capacity changes never call the port, because signups cannot exist before publication and the production default port is unavailable. The production default remains fail closed: Published capacity changes and activity removal return 503 until the registration owner provides a real adapter. Call probes verify invocation, transaction and rollback semantics only, not FIFO.

TDD: WIP RED evidence was reused. New RED observed before fixes: gate replacement (canEdit expected true, was false; Public-only expected 400, was 409) and Draft capacity (expected 200, was 503). Independent specification review: SPEC COMPLIANT. Independent quality review: APPROVED, no Critical/Important findings. Its Minor item about assertions on fixture-set probe values was fixed in 4cce076. Coordinator `mvnw verify` at 4cce076: 170 tests, 0 failures/errors/skips, BUILD SUCCESS (.cache/event-authoring/coordinator-issue21-final.log).

Open follow-ups, not blocking: (1) the last-activity rule and capacity-increase promotion should ignore cancelled activities once activity cancellation (#23, DuarteLagoela) exists; (2) GuardRequest carries no expectedVersion or new capacity, which is thinner than section F; agree on it with the registration owner; (3) EA-03 behavior: Draft activity removal still goes through the registration port, so it returns 503 in production; this needs a decision; (4) an invalid category returns 400 before authorization (no information leaks). Student review remains pending; no issue is closed.

## K. Decisions taken by the share owner (jose03pedro), 2026-11-10

The requesting student asked the agent to resolve the open follow-ups and take the decisions. They are recorded here as authoring-share decisions. Items that bind other owners are proposals until those owners accept them.

| ID | Decision | Rationale | Implementation / status |
|---|---|---|---|
| K-1 | A cancelled activity is read-only for authoring: update/remove activity and add/update/remove agenda return 409 STATE_CONFLICT. Precedence is 400 (null command or version) → 404 (unknown or foreign nested resource) → 403 (Staff) → 409 (event state or version) → 409 (cancelled), and the cancelled check comes before body validation. The Published last-activity rule counts only non-cancelled activities. Publication readiness requires at least one non-cancelled activity and skips cancelled ones. | BR-04, BR-06, BR-14. Cancellation cancels every signup, so editing a cancelled activity or requesting promotion for it has no meaning. Event state/version is checked before body validation everywhere else, so the cancelled check follows the same order. | ff5dbce. Cancellation itself stays with #23 (DuarteLagoela); tests simulate the cancelled flag. |
| K-2 | Removing a Draft activity deletes it and its agenda directly, without the registration port. Removing a Published activity keeps the guard. | A Draft has never been published, so it cannot have signups (BR-01), and the event never returns to Draft. Same reasoning as the Draft capacity correction. This removes the production 503 for Draft removal. | 9cca9e9 |
| K-3 | `GuardRequest(eventId, activityId, mutationKind, expectedVersion, requestedCapacity)`. Lock order: the Event row (PESSIMISTIC_WRITE, held by authoring) first, then the activity's signup and waitlist rows, then participant locks. Registration flows that change authoring data take the Event lock first. The adapter joins the caller's transaction and never commits or rolls back on its own. | Section F needs version and capacity context, and a single global lock order prevents deadlocks between authoring and signup flows. | 52059ab. **Proposal pending acceptance** by the registration owner (#31/#33, lucianvaleanu). Production stays fail closed (503) until a real adapter exists. |
| K-4 | Authoring Flyway migrations use the reserved range V2026111010–V2026111019. The initial migration was renamed to V2026111010. | Avoids the duplicate version with PR #58 (V2026111001/V2026111002). Nothing has been deployed, so renaming is safe. | 8649923. Local builds made before the rename may need `mvn clean`. |
| K-5 | An invalid category returns 400 before authorization. | The response does not depend on whether the event exists, and it matches the strict DTO deserialization used everywhere else. | No change |
| K-6 | Whichever of PR #58 and PR #59 merges second adopts the already-merged `frontend/` Vite host and keeps only its own `src/features/...` folder. The authoring shell host can be dropped in favor of the lifecycle scaffold. | Both PRs follow ADR-002 with the same toolchain. The authoring host is only a minimal shell. | Applied at merge time; no code change now. |
| K-7 | The authoring Event/Activity/Agenda model is canonical. Lifecycle's provisional model is replaced during reconciliation, as its own contract states. Lifecycle commands must operate on the authoring tables and respect K-3's Event-first lock. | Section J; PR #58 documents its model as provisional. | **Pending Duarte's acceptance**; reconciliation PR after one of #58/#59 merges. |
