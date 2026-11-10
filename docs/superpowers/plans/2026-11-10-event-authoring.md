# Mid-term Event Authoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development, explicitly selected by the user. Execute task-by-task with fresh implementers and independent spec then quality reviews. Steps use checkbox (`- [ ]`) tracking. The requesting student explicitly approved this written plan and execution in this session (2026-11-10). Routine tasks now proceed through SDD; no further plan-method approval is required.

**Goal:** Implement all safely executable backend, persistence, API and React authoring functionality for #17–#21, with honest pending-integration status for absent production identity/org/signup/search modules.

**Architecture:** The requesting student approved the isolated feature design and minimum setup on 2026-11-10. Use one authoring aggregate with nested activities/agendas, feature-scoped application services and explicit external capability ports. Keep production access and signup-sensitive commands fail closed when required adapters are absent. Existing agreed contracts with other owners take precedence; no such implementation contract was found in inspected branches.

**Tech Stack:** Java target 21, Spring Boot 4.1.1, Maven 3.9.16 via existing wrapper, Boot-managed JUnit Jupiter, PostgreSQL/JPA/Hibernate/Flyway, Spring Security, compatible OpenAPI, React/TypeScript/Vite/Router/Tailwind, Vitest/RTL and Playwright. Lock compatible unconfigured versions in Task EA-01; never force a JUnit 5 downgrade.

**Spec:** [Accepted requesting-student slice design](../specs/2026-11-10-event-authoring-design.md), [integration contract](../../integration/event-authoring-contract.md), [acceptance matrix](../../requirements/event-authoring-verification.md), approved US-14–18 and ADR-001–008.

## Global Constraints

- Scope is #17, #18, #19, #20 and #21, all assigned to jose03pedro. No Final features, cancellation, event deletion/completion, auth/org/team/signup/FIFO/discovery implementations or issue mutations.
- Main is e021b231a0018df82cffb59ac63e8f98688abf91. Work starts on feat/issue-17-jose03pedro-create-draft-event, then use dedicated/stacked issue branches for later story commits; no implementation on main, no force push or automatic merge/closure.
- Every repository artifact is English. Human Review Status remains Pending student review. Requesting-student design approval is real; independent Duarte approval and cross-owner contracts remain unconfirmed.
- Preserve Java 21, Boot 4.1.1 and Maven 3.9.16 where compatible. Installed JDK 21: /usr/lib/jvm/java-21-openjdk. Maven/dependencies are uncached; downloads and isolated containers are required for verification after approval.
- Do not use the unrelated opsrelay PostgreSQL service. Keep local tool caches under ignored repository .cache/; do not repurpose HOME or other system variables for unrelated purposes.
- Keep develop/frontend at 269375598446b545026ac100e6a790dcddb9fcaa untouched. Its Next.js starter is not the approved Vite foundation; only the newly approved minimum isolated Vite setup is owned here.
- Activity writes are complete; start < end, capacity >=1, at least location or online link. Private write capability is Final-only. Agenda endpoints are inclusive; reject schedule edits stranding agenda atomically.
- Keep external subject/org references opaque strings. Event.current owner is authoritative, initially creator; team adapter supplies Editor/Staff only. Owner/Editor/same-org Org Admin edit; Staff alone reads but never edits/publishes.
- Only Draft → Published is implemented. Completed/Cancelled reject authoring. No actor/owner/state/org reassignment mass assignment. Any signup history blocks removal; last Published activity removal is refused under the explicitly approved interpretation.
- Registration defaults and approved temporal interpretations are in contract H. Preserve signups when time changes introduce overlap; never implement notifications (#22) or FIFO (#33).

## Review Focus

1. Missing/untrusted identity or org/team adapter must never create/read/edit a draft: test principal, account eligibility, cross-org and nested-resource attacks in EA-02/03.
2. Concurrent/stale mutations must not bypass publication, capacity or signup constraints: aggregate version/locks in EA-02/04, actual PostgreSQL rollback tests in EA-04/07.
3. Time edits must not strand agendas or invalidate explicit registration closing; publication/added-activity times and original offsets must be stable: fixed-clock boundary tests in EA-03/04.
4. Production missing registration/discovery integrations must not be mistaken for passing doubles: 503/contract states in EA-04 and explicit evidence labels in EA-07.
5. UI stale responses, denied permissions, server readiness errors and failed writes must preserve user input and avoid false success: RTL tests in EA-05/06 and real-feature browser wiring in EA-07.

## File ownership and stable interfaces

Backend production root: backend/src/main/java/com/ubb/genai/events/authoring/. Test root mirrors it under backend/src/test/java/. Frontend feature root: frontend/src/features/event-authoring/. No worker reverts another contributor's work. Implementers are dispatched sequentially for shared mutable files.

API/domain conventions: EventState enum DRAFT/PUBLISHED/COMPLETED/CANCELLED; JSON lifecycle strings Draft/Published/Completed/Cancelled. EventCategory wires Workshop/Conference/Training/Social/Competition/Other; Visibility wires Public/Private, with Private writes refused. Local identifiers UUID; organizationRef/ownerSubject String. Public EventView excludes ownerSubject/account/email and includes id, organizationRef, title, description, category, state, createdAt, publishedAt, version, activities and capabilities(canEdit,canPublish). ActivityView contains its fields and sorted agenda. Request paths/bodies/statuses follow contract D; all successful writes return updated EventView and next version, DELETE version is in query.

Persistence schema event_authoring: event(id UUID PK, organization_ref TEXT, owner_subject TEXT, title TEXT, description TEXT, category TEXT, state TEXT, created_at TIMESTAMPTZ, published_at nullable TIMESTAMPTZ, version BIGINT); activity(id UUID PK, event_id UUID FK, title/description TEXT, starts_at/ends_at TIMESTAMPTZ, start_offset_seconds/end_offset_seconds INT, location/online_link nullable TEXT, capacity INT, registration_open/close nullable TIMESTAMPTZ, registration_open/close_offset_seconds nullable INT, visibility TEXT, cancelled BOOLEAN, created_at TIMESTAMPTZ); agenda_item(id UUID PK, activity_id UUID FK, time TIMESTAMPTZ, offset_seconds INT, title TEXT, speaker nullable TEXT). Required fields/approved enums and interval/capacity invariants use constraints; parent indexes support real queries. Root version/lock coordinates nested mutations. No external user/org FK or signup table is invented.

Stable application method names (command records carry the contract D fields): `createDraft(CreateEventCommand)`, `getEvent(UUID)`, `editEventDetails(UUID, EventDetailsCommand)`, `addActivity(UUID, ActivityCommand)`, `updateActivity(UUID, UUID, ActivityCommand)`, `removeActivity(UUID, UUID, long expectedVersion)`, `addAgenda(UUID, UUID, AgendaCommand)`, `updateAgenda(UUID, UUID, UUID, AgendaCommand)`, `removeAgenda(UUID, UUID, UUID, long expectedVersion)` and `publish(UUID, long expectedVersion)`, all returning EventView. Identity/authorization are resolved server-side, not method actor arguments supplied from HTTP. DTO-to-command mapping is explicit. `RegistrationAuthoringPort` exposes `<T> T withLockedActivity(GuardRequest request, Function<RegistrationContext,T> mutation)`. `GuardRequest` contains eventId/activityId/mutationKind; `RegistrationContext` exposes `long confirmedCount()`, `boolean hasAnySignup()` and `void promoteEligibleWaitlist(int newCapacity, Instant activityStartsAt, Instant now)`. Its scope is the real same-transaction guard; fixed test contexts only supply state and record calls.

External ports: AuthoringIdentityPort.currentActor() → Actor(subject, eligible) or typed unavailable/unauthenticated outcome; OrganizationAccessPort.forActor(subject,organizationRef) → current org role/membership result; EventTeamAccessPort.roleFor(subject,eventId) → Editor/Staff/none. No permissive production fallback. Use replaceable conditional beans that return unavailable for missing capabilities; a Spring session adapter may extract a trusted authenticated principal but must not invent account eligibility. Test identities/membership are only in test sources.

RegistrationAuthoringPort.withLockedActivity(GuardRequest, Function<RegistrationContext,T>) supplies the RegistrationContext defined above under an agreed transaction/lock protocol; authoring enforces capacity/deletion against it. The callback performs the authoring write and requests promoteEligibleWaitlist only for an eligible capacity increase before start. Real adapters must guarantee all-or-nothing same-transaction completion. Default is unavailable. Doubles provide fixed snapshots and record calls/failures; they do not implement signup/FIFO algorithms. Local Event-first locks are not advertised as a completed cross-module protocol.

## Commands and setup conventions

Existing backend checks run from the root with installed JDK 21 and task-local cache:

`rtk proxy env JAVA_HOME=/usr/lib/jvm/java-21-openjdk MAVEN_USER_HOME=.cache/event-authoring/maven-user bash backend/mvnw -f backend/pom.xml -Dmaven.repo.local=.cache/event-authoring/maven-repository test`

Use absolute cache paths if the wrapper/Maven resolves relative paths differently; keep them inside the official root. Task EA-01 must document the exact successful command and create no application feature outside this share. Backend test selection uses the existing Maven test goal and -Dtest=<class> where needed. Before running any new frontend command, EA-01 must actually configure scripts test, typecheck and build in frontend/package.json; planned invocations thereafter are `rtk proxy npm --prefix frontend run test -- --run`, `rtk proxy npm --prefix frontend run typecheck` and `rtk proxy npm --prefix frontend run build`. New commands are not claimed available before EA-01.

Every task: observed expected RED → minimal GREEN → meaningful targeted/regression checks → self-review → logical verified commit with issue reference → independent spec review → corrections/re-review → independent quality review → corrections/re-review → coordinator verification. Do not interpret import errors or infrastructure failures as a valid expected behavioral RED. Read the installed TDD/review templates and use actual commands/results, not assumed versions.

## EA-01 — Minimum isolated test/build and persistence foundation (#17–#21)

**Objective/requirements:** Approved minimum shared setup for the authoring slice; ADR-002/003/004/006, NFR-002/006. This does not complete account/org/M1 workflows.
**Dependencies:** Written-plan approval; requesting-student slice approval recorded. Preserve unrelated remote frontend work.
**Files:** modify backend/pom.xml and backend/src/test/java/com/ubb/genai/BackendApplicationTests.java for real test DB wiring; create root .gitignore; create backend/src/test/java/com/ubb/genai/events/authoring/support/PostgresTestConfiguration.java; create backend/src/test/java/com/ubb/genai/events/authoring/persistence/AuthoringMigrationTests.java; create backend/src/main/resources/db/migration/V2026111001__event_authoring_initial.sql; add minimum datasource/Flyway configuration in an authoring-owned resource file imported only as needed. Create frontend/package.json, lockfile, tsconfig files, index.html, vite.config.ts, src/main.tsx, src/App.tsx, src/index.css, src/test/setup.ts and minimum test configuration. Record every shared change in the contract.
**Interfaces produced:** selected schema above; real PostgreSQL test fixture reusable by later tasks; configured actual frontend test/typecheck/build commands. No production auth/org adapter, login screen, demo fake API or generic foundation feature is added.
- [x] Inspect/lock compatible dependencies using primary registries/docs; use Boot BOM for managed backend dependencies, including test infrastructure. Configure only necessary JPA/PostgreSQL/Flyway/security/OpenAPI/testing dependencies. Record exact added versions and migration collision check.
- [x] Write AuthoringMigrationTests first: real database information_schema must contain event/activity/agenda_item with parent FKs and strict interval/capacity checks; external references accept opaque strings. Initial schema-table assertion must fail before migration, with DB running and dependencies resolved.
- [x] Run targeted migration test and record expected RED (missing authoring tables), not a Docker/download error.
- [x] Add the migration and minimum resource/test wiring; rerun to GREEN and existing contextLoads against isolated PostgreSQL. No destructive schema updates or unrelated database reuse.
- [x] Configure the minimum isolated Vite feature host/toolchain. Before shell behavior, add an RTL shell test for the accessible authoring mount point/empty unavailable state, observe the missing behavior, then render the minimum host without fake product success. Configure actual test/typecheck/build scripts. Verify configured typecheck/build and test harness setup; do not write later feature behavior early.
- [x] Run backend tests, actual frontend checks and documentation validation; commit only verified setup with honest scope. Completion: schema/test/build foundations work; all external production integrations remain absent.

## EA-02 — Draft domain, authorization, persistence and API (#17)

**Requirements:** FR-EVT-01, BR-03, US-14; exactly three acceptance criteria.
**Dependencies:** EA-01. Production auth/org integrations remain unavailable; tests must state fixtures explicitly.
**Files:** create domain/EventState.java, EventCategory.java, ActivityVisibility.java, AuthoringEvent.java; ports/AuthoringIdentityPort.java, OrganizationAccessPort.java, EventTeamAccessPort.java; application/EventAuthoringService.java, AuthoringException.java; persistence/EventEntity.java, ActivityEntity.java, AgendaItemEntity.java, AuthoringEventRepository.java, JpaAuthoringRepository.java; api/EventAuthoringController.java, AuthoringRequests.java, AuthoringViews.java, AuthoringExceptionHandler.java; feature-owned fail-closed wiring/security configuration. Add domain/AuthoringEventTests.java, application/DraftAuthoringTests.java and api/DraftAuthoringApiTests.java.
**Consumed:** schema and PostgreSQL test configuration. **Produced:** createDraft(CreateEventCommand) → EventView; getEvent(UUID) → EventView; immutable current Owner reference initialized from trusted Actor; replaceable safe external ports; REST POST /api/event-authoring/events and GET /api/event-authoring/events/{id}.
- [x] Write observed failing tests for all US-14 criteria and Review Focus 1: required fields/category, Organizer/Org Admin eligibility, actual creator assignment, same-org Org Admin/Owner/team reads, unrelated/anonymous/Staff creation denial, missing adapter and client Owner/state injection. Use real domain/service and explicit fixture ports.
- [x] Implement only Draft creation/read, current Owner and nested-empty aggregate mapping, validation and transaction/optimistic-version infrastructure. Provide no global auth/membership/team implementation. Use injected Clock; store trusted identity only internally.
- [x] Add scoped security/CSRF checks and HTTP error handling, with no login/provider/password bypass. A production session without the required authoritative adapter must not gain access. Test cross-org/nested resource confidentiality and response redaction.
- [x] Verify targeted and regression backend tests against real PostgreSQL where persistence is involved. Commit #17 backend slice. Completion: real authoring persistence/API verified under identity/org fixtures; production account/org integration explicitly pending.

## EA-03 — Activity and agenda domain/API (#18/#19)

**Requirements:** FR-EVT-02/03, BR-07/08, US-15/16; six acceptance criteria.
**Dependencies:** EA-02; use stacked issue branches/commits so ownership remains explicit. Reconcile shared authoring files sequentially.
**Files:** extend domain/AuthoringEvent.java, application/EventAuthoringService.java, AuthoringRequests/Views/Controller and JPA mapping as needed; create domain/ActivitySchedule.java, RegistrationWindow.java; ports/RegistrationAuthoringPort.java with unavailable default for guarded operations; tests domain/ActivityScheduleTests.java, application/ActivityAuthoringTests.java, application/AgendaAuthoringTests.java, api/ActivityAgendaApiTests.java.
**Produced:** addActivity/updateActivity/removeActivity; addAgenda/updateAgenda/removeAgenda, each consuming parent ID, expectedVersion and typed fields and returning updated EventView. DELETE expectedVersion is query input. Preserve API prefix and endpoint inventory in contract D.
- [x] Write failing tests for complete valid activity writes, strict start < end, capacity integer >=1, location/link alternatives, optional window defaults, close <= start, explicit reversed/equal configured windows, preserved offsets and public-only writes.
- [x] Implement actual activity logic and API; no cancellation/private access. Refuse edits for Completed/Cancelled. Guard removal against signup history through the external seam; never fake zero in production.
- [x] Write failing agenda tests for required time/title, optional speaker, inclusive start/end, sorted reads, foreign-parent substitution, mutation authorization, and schedule changes stranding agenda/invalid explicit close. Implement atomic rejection without auto-rescheduling.
- [x] Verify service/MVC/SQL round trips and rollback; preserve field data and next versions. Commit separately for #18 and #19 on appropriate issue branches. Completion: activity/agenda rules locally verified; guarded real signup integration remains pending.

## EA-04 — Publication and Published editing/registration seam (#20/#21)

**Requirements:** FR-EVT-04/05, BR-01/02/06/08/12/14; US-17/18, eight criteria.
**Dependencies:** EA-03. Do not implement #22/#23/#25/#26/#33.
**Files:** extend AuthoringEvent, EventAuthoringService, Controller/DTOs and repository locks; create application/PublicationReadiness.java, ports/PublishedEventsReadContract.java; tests application/PublicationTests.java, application/PublishedEditingTests.java, api/PublicationApiTests.java, persistence/AuthoringConcurrencyTests.java, ports/RegistrationAuthoringContractTests.java.
**Produced:** publish(UUID,expectedVersion) → EventView; editEventDetails; guarded Published activity mutations. Committed read projection includes event/activity IDs, org reference, title/description/category, visibility/state/cancellation/windows, without Owner/account/email leakage. Organization name/activation/audience resolution remains with real owners.
- [ ] Write expected RED publication tests: no activities, every activity revalidated, accumulated problems, Owner/Editor/Org Admin allowed and Staff alone denied, repeated/non-Draft transition refused, original publication instant captured once, stale/concurrent publishes serialize.
- [ ] Implement atomic publication/readiness and projection only; no search implementation or success claim for missing consumer.
- [ ] Write expected RED Published editing tests: title/description/category and nested edits/additions, immutable org/current Owner, Completed/Cancelled read-only, agenda preserved/revalidated, last Published activity removal refused, unchanged signups on time overlap.
- [ ] Write registration-seam RED tests using explicit fixed snapshots/call probes: capacity below authoritative count refused; deletion with any history refused; before-start increase requests promotion in the same guarded transaction; after-start increase never promotes; missing/failed adapter refuses or rolls back; stale/concurrent mutation cannot bypass root checks. No local FIFO algorithm.
- [ ] Implement authoring checks and callbacks, fail-closed external adapter behavior and PostgreSQL rollback/version tests. Clearly document that the cross-module lock/FIFO protocol is not verified with real signup code.
- [ ] Commit verified local #20/#21 work with non-closing references where real acceptance remains incomplete. Completion: authoring behavior tested; #20 discoverability and #21 real promotion/concurrency are Pending integration, not complete stories.

## EA-05 — Event and activity React authoring (#17/#18)

**Requirements:** US-14/15; accessible real feature forms/client and status handling.
**Dependencies:** EA-01 tools and EA-02/03 backend contract; no production auth/org host exists.
**Files:** frontend/src/features/event-authoring/types.ts, api/eventAuthoringClient.ts, components/EventDetailsForm.tsx, components/ActivityForm.tsx, components/ActivityList.tsx, pages/EventAuthoringPage.tsx, index.ts and colocated *.test.tsx/*.test.ts. Update minimum App route registration only and document the shared change.
**Consumed:** EventView, command DTOs and server errors/expectedVersion. **Produced:** injected client/capability/session-CSRF integration and authoring routes /authoring/events/new and /authoring/events/:eventId. No login/org management/discovery frontend.
- [ ] Write failing RTL/client tests for every required event/activity field, explicit UTC labeling/offset payload, org selection reference, API success/400/401/403/409/422/503, credentials/CSRF handling, loading/empty state, retained input on errors, read-only Staff and stale version behavior. Tests use explicit API doubles; they are component tests, not real E2E.
- [ ] Implement real forms and feature client, no fake production success, no unnecessary state library. Backend responses supply next state/version/capabilities; frontend never supplies trusted user/Owner/state.
- [ ] Run configured Vitest, typecheck and Vite build plus relevant backend regressions. Commit #17/#18 UI changes in issue history. Completion: tested components/client; actual upstream session/org integration remains pending.

## EA-06 — Agenda, publication and Published-edit React flows (#19–#21)

**Requirements:** US-16/17/18; UI reflects actual accepted boundaries/errors.
**Dependencies:** EA-04/05.
**Files:** feature components/AgendaForm.tsx, AgendaList.tsx, PublishEventPanel.tsx, PublicationProblems.tsx; extend EventAuthoringPage/client/types and add colocated RTL tests.
**Produced:** accessible agenda CRUD, publication confirmation/readiness list, Published edits/additions and guarded capacity/removal feedback. No cancellation, draft deletion or Final notifications.
- [ ] Write failing RTL tests for agenda inclusive/outside ranges, optional speaker, schedule edit failures, complete publication problem list, Staff denial, repeated submission/loading, server version refresh after removal, and missing-registration 503/capacity 409 without claiming success.
- [ ] Implement UI using real API contract and server capabilities, preserving user input on rejected writes. Do not hide pending integration behind local optimistic success.
- [ ] Run all frontend tests/typecheck/build and targeted backend checks. Commit by issue where meaningful. Completion: all owned UI flows represented/tested, with absent real integrations accurately surfaced.

## EA-07 — Integration evidence, browser wiring and handoff (#17–#21)

**Requirements:** all 17 criteria, NFR-001/002/004/006 and DEL-008; integration/human review truthfulness.
**Dependencies:** EA-01–06 reviews passed.
**Files:** backend test-only support/AuthoringBrowserTestServer.java and fixture configuration if necessary; frontend/playwright.config.ts and e2e/event-authoring.spec.ts; docs/integration/event-authoring-contract.md, docs/requirements/event-authoring-verification.md, selected backlog/traceability entries, English feature run instructions and own UUID GenAI entry. Do not modify another developer's entry or auto-consolidate the shared index concurrently.
**Boundaries:** test-only server fixtures may seed a trusted session/org/roles and fixed signup snapshots only under src/test, on loopback and explicitly isolated infrastructure. They must not enter production classpath, production profiles or a public login bypass. If this cannot be made safely runnable, report the precise limitation and retain real backend/DB plus frontend component evidence; do not replace real-feature browser checks with an all-mocked E2E claim.
- [ ] Add failing meaningful integration tests for real Event API/PostgreSQL persistence, read/write role boundaries, version/transaction failures and API/UI error contract. Add Playwright only against a real authoring server/database when feasible; classify identity/org fixture use explicitly.
- [ ] Verify the real feature browser create→activity→agenda→publish path with a real database and actual frontend/backend. A fixture-based session is not evidence of account onboarding or real organization membership. No actual signup/FIFO/discovery claim without those services.
- [ ] Run backend tests/verify, frontend test/typecheck/build, configured browser test, documentation validation and git diff --check. Record skipped/failing checks honestly and resolve actual authoring defects through TDD, not by deleting tests or weakening security.
- [ ] Update all 17 matrix rows with actual file/test names, result timestamps and real/double/pending classifications; contract A–I with actual endpoints, schema/migrations, shared files and unresolved integration changes. Keep incomplete issues Partial/Pending integration, with no false closure.
- [ ] Independent whole-share spec, architecture/quality and integration reviews inspect actual code and evidence after task-level reviews. Resolve/re-review findings; coordinator reruns affected checks.
- [ ] Commit only verified changes; push owned issue/share branch and open reviewable PR against main if ready. Use non-closing Refs/Related to #17–#21 unless full real acceptance exists. Do not merge, close issues, force push or claim student review completed. Finish with actual branch/commit/PR, all five statuses, actual subagents/reviews, commands/results and actionable handoff.

## Self-review and execution gate

This single plan covers all five issues, 17 criteria and the schema/API/UI/test/integration surfaces. Detailed signatures above are shared inputs for fresh workers; refine only with documented evidence, without conflicting plans or silent contract changes. The requesting student selected SDD; do not ask them to choose another execution method. **Written-plan review completed: the requesting student explicitly selected “Approve and execute the plan” on 2026-11-10.** Approval of this plan authorizes routine configured setup downloads and project-isolated test containers, not access to unrelated services or changes to other contributors' branches.

## Execution record

- Requesting-student slice approval: confirmed.
- Written-plan approval and SDD execution: confirmed.
- EA-01: Implementer committed f41c52a with reported RED/GREEN evidence; independent spec review requested corrections. Interrupted correction resumed on 2026-11-10: 32 migration tests observed with 12 expected assertion failures and no errors, demonstrating whitespace and timestamp/offset-pair gaps. Correction f967285 passed independent spec and quality reviews and coordinator checks: 34 backend tests, 1 RTL test, typecheck/build. Minor Vitest/TypeScript alignment resolved in 2b8163f and independently re-reviewed; coordinator repeated all frontend checks successfully. EA-01 complete.
- EA-02: Complete at 7263e0c after independent spec and quality correction/re-review loops. Coordinator 95 backend tests passed; actual upstream adapters and UI remain pending.
- EA-03: Complete at 4ecc06f. Activity a9e7701, agenda aa6fd30, nested-resolution fix 14dbf39 and coherent-read fix 4ecc06f. Independent Terra specification and quality re-reviews approved; coordinator 123 backend tests passed. Existing branch/history and issue18/19 checkpoint refs preserved. No real upstream/signup integration or full-story delivery claimed.
- EA-04–07: Not Started.
- Independent peer/cross-owner integration acceptance and student implementation review: pending; no production integration is fabricated.

Resumption routing constraint: the user explicitly prohibited Astra subagents. Future implementation and review workers use other available models; completed earlier reviews remain historical evidence.
