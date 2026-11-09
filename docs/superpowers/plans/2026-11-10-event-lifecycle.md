# Event Lifecycle, Search, Feedback and Reporting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the DuarteLagoela share (issues #23 US-20, #24 US-21, #25 US-22, #26 US-23, #28 US-24, #29 US-25, #54 US-47, #55 US-48, #56 US-49, #57 US-50) as an isolated backend module and frontend feature folder. The module depends on stubbed interfaces for the features other teammates own.

**Architecture:** One Spring feature module, `com.ubb.genai.eventlifecycle`, with sub-packages `shared` (provisional event/activity model, ports to other shares, stubs, access policy, errors), `lifecycle`, `search`, `feedback` and `reports`. Each sub-package keeps controller → service → repository boundaries, and business rules live in pure, unit-tested classes. A Vite/React SPA lives in `frontend/`, with all of this share's UI under `frontend/src/features/event-lifecycle/`. Every API path is prefixed per share (`/api/lifecycle`, `/api/discovery`, `/api/feedback`, `/api/reports`) so it cannot collide with the authoring share.

**Tech Stack:** Java 21, Spring Boot 4.1.1 (Web MVC, Validation, Data JPA, Flyway), PostgreSQL, JUnit Jupiter, Mockito, Spring Boot Test, Testcontainers; React + TypeScript + Vite + React Router + Tailwind CSS, Vitest + React Testing Library.

**Spec:** `docs/requirements/02-requirements.md`, `04-use-cases.md` (UC-13, UC-14, UC-15, UC-19, UC-29, UC-30), `05-workflows.md` (WF-02, WF-03, WF-06), `08-user-stories.md` (US-20–US-25, US-47–US-50), `03-actors-and-roles.md`; ADR-001–007 in `docs/decisions/`.

## Global Constraints

- Every repository artifact is written in English (code, identifiers, comments, tests, docs, commits, PR).
- Every test name contains a US or BR ID: Java method names start with the lower-case ID (`us20_…`, `br05_…`) and carry `@DisplayName("US-20 …")`; Vitest `it("US-24 …")`.
- Do not edit code outside `backend/src/main/java/com/ubb/genai/eventlifecycle/`, `backend/src/test/java/com/ubb/genai/eventlifecycle/`, `backend/src/main/resources/db/migration/eventlifecycle/` and `frontend/src/features/event-lifecycle/`. The allowed shared files are only `backend/pom.xml`, `backend/src/main/resources/application.properties`, `backend/src/test/resources/application.properties` and the `frontend/` scaffold. List each of them in the contract file.
- The provisional model is marked provisional in each class's Javadoc: `Provisional: owned by the event-authoring share (jose03pedro); replaced at reconciliation.`
- Table names start with `lifecycle_`. Flyway migrations live in `db/migration/eventlifecycle/` with timestamp versions `V2026111001__…`, `V2026111002__…`.
- Instants are stored as `timestamptz` and handled as `java.time.Instant` (UTC). All "now" logic goes through an injected `java.time.Clock`.
- Ids are `UUID` for events, activities, organizations, users and feedback.
- Event categories, exact values: `WORKSHOP, CONFERENCE, TRAINING, SOCIAL, COMPETITION, OTHER` (display: Workshop, Conference, Training, Social, Competition, Other).
- Event states: `DRAFT, PUBLISHED, COMPLETED, CANCELLED` (BR-01). Activity states: `SCHEDULED, CANCELLED`. Visibility: `PUBLIC, PRIVATE`.
- Errors are returned as RFC 9457 `ProblemDetail` with an extra `code` property (UPPER_SNAKE). Status mapping: 400 validation, 401 no user, 403 insufficient role, 404 not found or hidden, 409 rule or state conflict.
- Feedback and report responses never contain user ids, usernames or emails (BR-17, BR-19, US-50).
- Each task updates `docs/integration/event-lifecycle-contract.md` with what it added (model, tables, endpoints, ports, assumptions).
- Each commit message references its issue number(s), e.g. `feat(lifecycle): cancel an activity (#23)`.
- Backend commands: `bash backend/mvnw -f backend/pom.xml test` (the wrapper is not executable; `rtk` is not installed here). Tests that touch persistence need Docker running for Testcontainers.

## Decided assumptions (record these in the contract)

- **Time boundaries (Q-05):** an activity has ended when `now >= endsAt`. The feedback window is `endsAt <= now <= endsAt + 7 days`, inclusive at both ends. An activity is cancellable while `now < endsAt`.
- **Attendance rate (Q-08):** `attended / (confirmed + attended)`, where `confirmed` counts signups currently Confirmed (not yet marked present). The result is `null` (CSV: empty cell) when the denominator is 0. The report's "confirmed signups" value is `confirmed + attended`.
- **Search "upcoming":** an event is listed when it is PUBLISHED and has at least one activity that is non-cancelled, visible to the viewer and has `endsAt > now`. It is sorted by the earliest `startsAt` among those activities, ascending, with ties broken by title and then id. Private activities the viewer cannot see never contribute to listing, sorting or date filtering.
- **Date filter:** the `from`/`to` instants match an event when one of its qualifying activities overlaps them (`startsAt <= to` and `endsAt >= from`). Either bound may be omitted.
- **Keyword:** trimmed. Blank means no keyword. It is matched case-insensitively as a literal substring against the title, the description, or the organization name (via the `OrganizationDirectory` port).
- **Visibility to a non-team user:** a DRAFT event is answered as 404, never 403 (BR-03).
- **Authorization:** "owner rights" means event role OWNER or Org Admin of the event's organization. "Team member" means any event role (OWNER, EDITOR, STAFF) or Org Admin of the organization.
- **Current user (provisional):** taken from the `X-User-Id` request header (a UUID). If the header is missing, the caller is a guest. The real authentication share replaces this.
- **Notifications:** the lifecycle services call `NotificationPort` after the state change. The stub logs ids only, never emails.
- **Search runs in memory:** candidate published events with upcoming activities are loaded in one fetch-join query, then filtered, sorted and paged in Java. This is acceptable at course scale; the contract notes it as a known limitation.

## Review Focus

1. A keyword with surrounding whitespace, mixed case or the characters `%` and `_` is matched literally and case-insensitively. A blank keyword lists everything. (Task 6 test `us24_keywordIsTrimmedCaseInsensitiveAndLiteral`.)
2. Cancelling an already cancelled activity or event, an ended activity, or a completed event returns 409 with a `code`, not 500, and does not cancel signups or notify a second time. (Task 3 tests `br06_cancellingCancelledEventIsRefused`, `us20_cancellingEndedActivityIsRefused`.)
3. A guest viewing an event with both public and private activities sees it listed, but its sort key and date match come only from the public activities. An event whose activities are all private is hidden. (Task 6 test `br18_privateActivitiesDoNotAffectSortOrDateFilterForGuests`.)
4. Feedback exactly at `endsAt` and exactly at `endsAt + 7d` is accepted. Feedback at `+7d +1s`, or before the activity ends, is refused. (Task 7 test `br16_feedbackWindowBoundaries`.)
5. CSV cells containing commas, quotes or newlines, or starting with `=`, `+`, `-` or `@`, are RFC 4180 quoted and formula-neutralised. (Task 9 test `us50_csvEscapesAndNeutralisesFormulas`.)

---

### Task 1: Persistence foundation and provisional model (BR-01)

**Files:**
- Modify: `backend/pom.xml`: uncomment `spring-boot-starter-data-jpa` and `spring-boot-starter-data-jpa-test`; add `spring-boot-starter-flyway`, `org.flywaydb:flyway-database-postgresql`, `org.postgresql:postgresql` (runtime), `org.springframework.boot:spring-boot-testcontainers` (test), and the Testcontainers PostgreSQL and JUnit Jupiter modules (test, versions managed by the Boot BOM; check the artifact ids against the BOM).
- Modify: `backend/src/main/resources/application.properties`: `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/ubb_events}`, `spring.datasource.username=${DB_USERNAME:ubb}`, `spring.datasource.password=${DB_PASSWORD:}`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`.
- Create: `backend/src/test/resources/application.properties`: `spring.datasource.url=jdbc:tc:postgresql:17-alpine:///ubb_events_test` plus `spring.jpa.hibernate.ddl-auto=validate`. With this file the existing `BackendApplicationTests` runs unmodified.
- Create: `backend/src/main/resources/db/migration/eventlifecycle/V2026111001__lifecycle_create_event_activity_agenda.sql`
- Create in `backend/src/main/java/com/ubb/genai/eventlifecycle/shared/model/`: `EventState.java`, `ActivityState.java`, `ActivityVisibility.java`, `EventCategory.java`, `LifecycleEvent.java`, `LifecycleActivity.java`, `LifecycleAgendaItem.java`
- Create in `.../eventlifecycle/shared/persistence/`: `LifecycleEventRepository.java`, `LifecycleActivityRepository.java`
- Create: `backend/src/main/java/com/ubb/genai/eventlifecycle/shared/EventLifecycleConfiguration.java` (`@Bean @ConditionalOnMissingBean Clock clock()` returning `Clock.systemUTC()`)
- Test: `backend/src/test/java/com/ubb/genai/eventlifecycle/shared/model/LifecycleEventTest.java` (pure unit)
- Test: `backend/src/test/java/com/ubb/genai/eventlifecycle/shared/persistence/LifecycleEventRepositoryTest.java` (`@DataJpaTest` with `@AutoConfigureTestDatabase(replace = NONE)`)
- Test: `backend/src/test/java/com/ubb/genai/eventlifecycle/EventLifecycleFixtures.java` (test fixture builder; it is not a test class)
- Create: `docs/integration/event-lifecycle-contract.md` (initial: purpose, shared-file changes, model, tables)

**Schema (migration):**
- `lifecycle_event`: `id uuid pk`, `organization_id uuid not null`, `title varchar(200) not null`, `description text not null`, `category varchar(20) not null check (category in (...6 values))`, `state varchar(20) not null check (state in ('DRAFT','PUBLISHED','COMPLETED','CANCELLED'))`, `published_at timestamptz null`, `cancelled_at timestamptz null`, `completed_at timestamptz null`, `version bigint not null default 0`. Index on `(state)`.
- `lifecycle_activity`: `id uuid pk`, `event_id uuid not null references lifecycle_event(id) on delete cascade`, `title varchar(200) not null`, `description text not null default ''`, `starts_at timestamptz null`, `ends_at timestamptz null`, `location varchar(300) null`, `online_link varchar(500) null`, `capacity int null check (capacity >= 1)`, `registration_opens_at timestamptz null`, `registration_closes_at timestamptz null`, `visibility varchar(10) not null default 'PUBLIC' check (...)`, `state varchar(10) not null default 'SCHEDULED' check (state in ('SCHEDULED','CANCELLED'))`, `cancelled_at timestamptz null`, `check (ends_at is null or starts_at is null or ends_at > starts_at)`. Index on `(event_id)`.
- `lifecycle_agenda_item`: `id uuid pk`, `activity_id uuid not null references lifecycle_activity(id) on delete cascade`, `starts_at timestamptz not null`, `title varchar(200) not null`, `speaker varchar(200) null`.

**Interfaces:**
- Produces (entities, JPA, field names match the columns in camelCase; `@Version long version` on the event):
  - `LifecycleEvent`: getters for every field; `List<LifecycleActivity> getActivities()` (`@OneToMany(mappedBy="event", cascade=ALL, orphanRemoval=true)`); a protected no-arg constructor; a static factory `LifecycleEvent.draft(UUID organizationId, String title, String description, EventCategory category)` that generates the id; `void addActivity(LifecycleActivity a)`; and the domain transitions:
    - `void publish(Instant now)`: DRAFT → PUBLISHED. Any other state throws `IllegalEventTransitionException`. Used only by fixtures; publishing rules (BR-02) belong to the authoring share.
    - `void cancel(Instant now)`: DRAFT or PUBLISHED → CANCELLED; cancels every SCHEDULED activity. Any other state throws.
    - `void complete(Instant now)`: PUBLISHED → COMPLETED. Any other state throws.
    - `boolean isReadOnly()`: true for COMPLETED and CANCELLED.
    - `boolean allActivitiesCancelled()`: true when there is at least one activity and every activity is CANCELLED.
    - `Optional<Instant> lastScheduledActivityEnd()`: max `endsAt` over SCHEDULED activities.
  - `LifecycleActivity`: `static LifecycleActivity scheduled(String title, Instant startsAt, Instant endsAt, ActivityVisibility visibility)`; setters for `location`, `onlineLink`, `capacity`; `void cancel(Instant now)` (CANCELLED → throws `IllegalEventTransitionException`); `boolean isCancelled()`; `boolean hasEnded(Instant now)` (`endsAt != null && !now.isBefore(endsAt)`); `UUID getEventId()`.
  - `LifecycleAgendaItem(LifecycleActivity activity, Instant startsAt, String title, String speaker)`.
  - `IllegalEventTransitionException extends RuntimeException` in `shared/model`, with `String code()` returning e.g. `EVENT_NOT_CANCELLABLE`, `ACTIVITY_ALREADY_CANCELLED`, `EVENT_NOT_COMPLETABLE`.
  - `LifecycleEventRepository extends JpaRepository<LifecycleEvent, UUID>`; `LifecycleActivityRepository extends JpaRepository<LifecycleActivity, UUID>`.
  - `EventLifecycleFixtures` (test): `static LifecycleEvent publishedEvent(UUID orgId, Instant start, Duration length, ActivityVisibility... activities)` and `static LifecycleEvent draftEvent(UUID orgId)`. One activity per visibility vararg, each starting 1 hour after the previous one.

- [ ] **Step 1: Write failing unit tests in `LifecycleEventTest`**
  - `br01_draftCanBePublished`: draft → `publish(now)` → state PUBLISHED and `publishedAt == now`.
  - `br01_draftAndPublishedCanBeCancelled`: both reach CANCELLED, and every activity becomes CANCELLED with `cancelledAt == now`.
  - `br01_completedOrCancelledEventCannotBeCancelled`: `assertThatThrownBy(...).isInstanceOf(IllegalEventTransitionException.class)` with code `EVENT_NOT_CANCELLABLE`.
  - `br01_onlyPublishedEventCanComplete`: draft.complete throws `EVENT_NOT_COMPLETABLE`; published.complete → COMPLETED.
  - `br06_completedAndCancelledEventsAreReadOnly`: `isReadOnly()` is false for DRAFT and PUBLISHED, true for the others.
  - `br04_lastScheduledActivityEndIgnoresCancelledActivities`: activities ending at 10:00 (cancelled) and 09:00 → `Optional.of(09:00)`.
- [ ] **Step 2: Run** `bash backend/mvnw -f backend/pom.xml test -Dtest=LifecycleEventTest`. Expected: compilation failure (classes missing).
- [ ] **Step 3: Implement the enums, entities, exception and migration listed above.**
- [ ] **Step 4: Run the same command.** Expected: 6 tests PASS.
- [ ] **Step 5: Write the failing persistence test `LifecycleEventRepositoryTest`**
  - `br01_eventWithActivitiesAndAgendaRoundTrips`: save a published fixture with 2 activities and 1 agenda item, flush and clear, reload: state, category and activity count are equal; the migration validates against the entities (Hibernate `validate`).
  - `br01_databaseRejectsUnknownState`: a native insert with `state='ARCHIVED'` throws `DataIntegrityViolationException`.
- [ ] **Step 6: Run** `bash backend/mvnw -f backend/pom.xml test`. Expected: all tests PASS, including the existing `BackendApplicationTests.contextLoads` (Docker must be running).
- [ ] **Step 7: Write the contract file sections "Shared files changed", "Provisional model", "Tables and migrations".**
- [ ] **Step 8: Commit** `feat(lifecycle): add provisional event model and persistence foundation (#23 #24 #25 #26)`

---

### Task 2: Ports to other shares, stubs, current user, access policy, error handling (BR-03, BR-18)

**Files:**
- Create in `.../eventlifecycle/shared/port/`:
  - `CurrentUserProvider.java`: `Optional<UUID> currentUserId()`
  - `OrganizationDirectory.java`: `boolean isMember(UUID organizationId, UUID userId)`, `boolean isOrgAdmin(UUID organizationId, UUID userId)`, `Optional<String> organizationName(UUID organizationId)`, `Set<UUID> organizationIdsWithNameContaining(String keyword)` (case-insensitive)
  - `EventTeamDirectory.java`: `Optional<EventRole> roleOf(UUID eventId, UUID userId)`; `enum EventRole { OWNER, EDITOR, STAFF }` in the same package
  - `PrivateActivityInvitations.java`: `boolean isInvited(UUID activityId, UUID userId)`
  - `SignupPort.java`: `Set<UUID> cancelAllSignups(UUID activityId)` (returns the users who held a Confirmed or Waitlisted signup), `SignupFigures figures(UUID activityId)`, `boolean wasMarkedPresent(UUID activityId, UUID userId)`; `record SignupFigures(int confirmed, int waitlisted, int cancelled, int attended)`
  - `NotificationPort.java`: `void activityCancelled(UUID activityId, Set<UUID> recipients)`, `void eventCancelled(UUID eventId, Set<UUID> recipients)`
- Create in `.../eventlifecycle/shared/port/stub/`: one `@Component` per port, named `Stub<Port>`, each with the Javadoc line `Stub: replace with the real implementation from <owner share> at reconciliation.` Behaviour: `HeaderCurrentUserProvider` reads `X-User-Id` through `RequestContextHolder` and returns empty on a missing or malformed header; the directory stubs return false, empty or `Optional.empty()`; `StubSignupPort` returns an empty set and zero figures; `StubNotificationPort` logs ids at INFO.
- Create: `.../eventlifecycle/shared/access/EventAccessPolicy.java` (`@Component`)
- Create: `.../eventlifecycle/shared/web/LifecycleProblem.java`: `class LifecycleProblem extends RuntimeException { LifecycleProblem(HttpStatus status, String code, String message) }` with static factories `notFound(code)`, `forbidden(code)`, `unauthenticated()`, `conflict(code, message)`
- Create: `.../eventlifecycle/shared/web/EventLifecycleExceptionHandler.java`: `@RestControllerAdvice(basePackages = "com.ubb.genai.eventlifecycle")` mapping `LifecycleProblem` → its status, `IllegalEventTransitionException` → 409, `MethodArgumentNotValidException` / `MethodArgumentTypeMismatchException` / `HandlerMethodValidationException` → 400 `VALIDATION_FAILED`, and `ObjectOptimisticLockingFailureException` → 409 `CONCURRENT_MODIFICATION`. The body is a `ProblemDetail` with the property `code`.
- Test: `backend/src/test/java/com/ubb/genai/eventlifecycle/shared/access/EventAccessPolicyTest.java` (Mockito, no Spring)

**Interfaces:**
- Consumes: Task 1 entities.
- Produces (`EventAccessPolicy`):
  - `boolean hasOwnerRights(LifecycleEvent event, UUID userId)`: role OWNER or Org Admin.
  - `boolean isTeamMember(LifecycleEvent event, UUID userId)`: any role or Org Admin.
  - `boolean canSeeEvent(LifecycleEvent event, Optional<UUID> userId)`: non-DRAFT, or a team member (BR-03).
  - `boolean canSeeActivity(LifecycleActivity activity, UUID organizationId, Optional<UUID> userId)`: PUBLIC, or the user is an organization member, or the user is invited (BR-18).
  - `UUID requireUser(Optional<UUID> userId)`: throws `LifecycleProblem.unauthenticated()` (401, code `UNAUTHENTICATED`).
  - `LifecycleEvent requireVisibleEvent(UUID eventId, Optional<UUID> userId)`: 404 `EVENT_NOT_FOUND` when missing or not visible.
  - `void requireOwnerRights(LifecycleEvent e, UUID userId)`: 403 `OWNER_RIGHTS_REQUIRED`. `void requireTeamMember(LifecycleEvent e, UUID userId)`: 403 `TEAM_MEMBER_REQUIRED`.

- [ ] **Step 1: Write failing tests in `EventAccessPolicyTest`**
  - `br03_draftHiddenFromNonTeamUsersAndGuests`: a draft with no role and not Org Admin → `canSeeEvent` is false for that user and for `Optional.empty()`.
  - `br03_draftVisibleToTeamAndOrgAdmins`: role STAFF → true; `isOrgAdmin` → true.
  - `br18_privateActivityVisibleOnlyToMembersAndInvitees`: PRIVATE: guest false, stranger false, member true, invitee true; PUBLIC: guest true.
  - `us20_ownerRightsForOwnerAndOrgAdminOnly`: EDITOR → false, STAFF → false, OWNER → true, Org Admin → true.
  - `us48_teamMemberIncludesStaff`: STAFF → `isTeamMember` true; no role → false.
- [ ] **Step 2: Run** `bash backend/mvnw -f backend/pom.xml test -Dtest=EventAccessPolicyTest`. Expected: compilation failure.
- [ ] **Step 3: Implement the ports, stubs, policy, `LifecycleProblem` and the exception handler.**
- [ ] **Step 4: Run** `bash backend/mvnw -f backend/pom.xml test`. Expected: all PASS (the context still loads with the stubs).
- [ ] **Step 5: Add the contract section "Interfaces stubbed" (one table row per port: method, semantics, owning share, stub behaviour) plus "Authorization assumptions" and "Error format".**
- [ ] **Step 6: Commit** `feat(lifecycle): add ports to other shares, stubs and access policy (#23 #28)`

---

### Task 3: Cancel an activity and cancel an event (US-20 #23, US-21 #24; BR-04, BR-05, BR-06)

**Files:**
- Create: `.../eventlifecycle/lifecycle/EventCancellationService.java`
- Create: `.../eventlifecycle/lifecycle/LifecycleController.java`
- Create: `.../eventlifecycle/lifecycle/LifecycleEventView.java`: `record LifecycleEventView(UUID id, String title, EventState state, List<ActivityView> activities, Permissions permissions)`, with nested `record ActivityView(UUID id, String title, Instant startsAt, Instant endsAt, ActivityState state)` and `record Permissions(boolean canCancel, boolean canDelete)`
- Test: `backend/src/test/java/com/ubb/genai/eventlifecycle/lifecycle/EventCancellationServiceTest.java` (`@SpringBootTest` + Testcontainers via the test properties; `@MockitoBean` for `SignupPort`, `NotificationPort`, `EventTeamDirectory`, `OrganizationDirectory`; a `@TestConfiguration` fixed `Clock`)
- Test: `backend/src/test/java/com/ubb/genai/eventlifecycle/lifecycle/LifecycleControllerTest.java` (`@WebMvcTest(LifecycleController.class)` with `@MockitoBean` service and `CurrentUserProvider`)

**Interfaces:**
- Consumes: `EventAccessPolicy`, `SignupPort`, `NotificationPort`, the repositories, `Clock`.
- Produces:
  - `EventCancellationService.cancelActivity(UUID activityId, Optional<UUID> userId): LifecycleEventView` (`@Transactional`). Rules: activity missing → 404 `ACTIVITY_NOT_FOUND`; event not visible → 404; not owner rights → 403; event not DRAFT/PUBLISHED → 409 `EVENT_READ_ONLY`; activity already cancelled → 409 `ACTIVITY_ALREADY_CANCELLED`; activity ended → 409 `ACTIVITY_ENDED`. Effects: activity CANCELLED; `signupPort.cancelAllSignups(activityId)`; if `event.allActivitiesCancelled()` then `event.cancel(now)` (BR-04); `notificationPort.activityCancelled(activityId, recipients)` (also `eventCancelled(eventId, recipients)` when the cascade fired). The event entity is saved so `@Version` increments.
  - `EventCancellationService.cancelEvent(UUID eventId, Optional<UUID> userId): LifecycleEventView`. Rules: 404, 403 as above; not DRAFT/PUBLISHED → 409 `EVENT_NOT_CANCELLABLE`. Effects: `cancelAllSignups` for every activity that was SCHEDULED before the cancel; `event.cancel(now)`; `eventCancelled(eventId, unionOfRecipients)`.
  - `EventCancellationService.view(UUID eventId, Optional<UUID> userId): LifecycleEventView`: 404 if not visible. `canCancel` = owner rights && !readOnly; `canDelete` = owner rights && DRAFT.
  - HTTP (`LifecycleController`, base `/api/lifecycle`): `GET /events/{eventId}` → 200 view; `POST /events/{eventId}/cancel` → 200 view; `POST /activities/{activityId}/cancel` → 200 view. The user comes from `CurrentUserProvider.currentUserId()`.

- [ ] **Step 1: Write failing service tests**
  - `us20_cancellingActivityCancelsItsSignupsAndWaitlist`: owner cancels activity A of a 2-activity published event → A is CANCELLED, B is SCHEDULED, event is PUBLISHED; `verify(signupPort).cancelAllSignups(A)`; `verify(notificationPort).activityCancelled(A, recipients)`.
  - `br04_cancellingLastScheduledActivityCancelsEvent`: cancel the only remaining activity → event CANCELLED; `eventCancelled` invoked.
  - `us20_editorsAndStaffCannotCancelActivity`: EDITOR → `LifecycleProblem` with status 403; STAFF → 403; `verifyNoInteractions(signupPort)`.
  - `us20_cancellingEndedActivityIsRefused`: clock after `endsAt` → 409 `ACTIVITY_ENDED`; no signup interaction.
  - `us20_cancellingCancelledActivityIsRefused`: → 409 `ACTIVITY_ALREADY_CANCELLED`; no second notification.
  - `us21_cancellingEventCancelsAllActivitiesAndSignups`: published event with 3 activities, one already cancelled → event CANCELLED; `cancelAllSignups` is called exactly for the 2 scheduled ones; `eventCancelled` is called once with the union of recipients.
  - `us21_draftEventCanBeCancelled`: draft → CANCELLED.
  - `br06_cancellingCancelledEventIsRefused` and `br06_cancellingCompletedEventIsRefused`: → 409 `EVENT_NOT_CANCELLABLE`.
  - `br03_cancellingHiddenDraftAnswersNotFound`: stranger cancels a draft → 404.
- [ ] **Step 2: Run** `bash backend/mvnw -f backend/pom.xml test -Dtest=EventCancellationServiceTest`. Expected: compilation failure.
- [ ] **Step 3: Implement `EventCancellationService` and `LifecycleEventView`.**
- [ ] **Step 4: Run the same command.** Expected: PASS.
- [ ] **Step 5: Write failing controller tests**
  - `us21_cancelEventEndpointReturnsView`: `POST /api/lifecycle/events/{id}/cancel` with the service stubbed → 200, `$.state == "CANCELLED"`.
  - `us20_cancelActivityEndpointMapsConflictToProblem`: the service throws `conflict("ACTIVITY_ENDED", …)` → 409, `$.code == "ACTIVITY_ENDED"`, content type `application/problem+json`.
  - `us21_cancelWithoutUserReturns401`: the service throws `unauthenticated()` → 401.
  - `us20_malformedIdReturns400`: `/api/lifecycle/activities/not-a-uuid/cancel` → 400 `VALIDATION_FAILED`.
- [ ] **Step 6: Implement `LifecycleController`; run** `bash backend/mvnw -f backend/pom.xml test`. Expected: all PASS.
- [ ] **Step 7: Add the contract endpoints (request, response JSON example, errors) and the BR-04 cascade semantics.**
- [ ] **Step 8: Commit** `feat(lifecycle): cancel activities and events with cascades (#23 #24)`

---

### Task 4: Delete a draft event and expose the read-only guard (US-22 #25; BR-05, BR-06)

**Files:**
- Create: `.../eventlifecycle/lifecycle/DraftDeletionService.java`
- Create: `.../eventlifecycle/lifecycle/EventReadOnlyGuard.java`: a public API for the authoring and attendance shares
- Modify: `.../eventlifecycle/lifecycle/LifecycleController.java`: add `DELETE /api/lifecycle/events/{eventId}` → 204
- Test: `.../lifecycle/DraftDeletionServiceTest.java` (`@SpringBootTest`), `.../lifecycle/EventReadOnlyGuardTest.java` (unit), and an extra case in `LifecycleControllerTest`

**Interfaces:**
- Produces:
  - `DraftDeletionService.deleteDraft(UUID eventId, Optional<UUID> userId): void` (`@Transactional`): 404 if not visible; 403 without owner rights; not DRAFT → 409 `EVENT_NOT_DRAFT` with the message "Published events cannot be deleted, only cancelled." Deletes the event; activities and agenda items cascade at both the JPA and the database level.
  - `EventReadOnlyGuard.requireEditable(UUID eventId): void`: 404 `EVENT_NOT_FOUND` if missing; 409 `EVENT_READ_ONLY` if `isReadOnly()`. Its Javadoc states that attendance and feedback deliberately skip this guard (BR-06 exception).

- [ ] **Step 1: Write failing tests**
  - `us22_draftIsDeletedWithActivitiesAndAgendaItems`: draft with 2 activities and 3 agenda items → after the delete, `count()` is 0 for the event, activity and agenda tables (query the agenda table via `JdbcTemplate`).
  - `us22_publishedEventCannotBeDeleted`: → 409 `EVENT_NOT_DRAFT`; the event still exists.
  - `br05_cancelledAndCompletedEventsCannotBeDeleted`: → 409 `EVENT_NOT_DRAFT`.
  - `us22_onlyOwnerRightsMayDelete`: EDITOR → 403.
  - `br06_readOnlyGuardRefusesCompletedAndCancelled`, `br06_readOnlyGuardAllowsDraftAndPublished` (unit, with a mocked repository).
  - Controller `us22_deleteEndpointReturns204`.
- [ ] **Step 2: Run** `bash backend/mvnw -f backend/pom.xml test -Dtest='DraftDeletionServiceTest,EventReadOnlyGuardTest'`. Expected: compilation failure.
- [ ] **Step 3: Implement both classes and the endpoint.**
- [ ] **Step 4: Run** `bash backend/mvnw -f backend/pom.xml test`. Expected: all PASS.
- [ ] **Step 5: Add the contract entries for the delete endpoint and for `EventReadOnlyGuard` as a provided interface that other shares should call before edits.**
- [ ] **Step 6: Commit** `feat(lifecycle): delete draft events and guard read-only events (#25 #24)`

---

### Task 5: Complete events automatically (US-23 #26; BR-01, BR-04, BR-06)

**Files:**
- Create: `.../eventlifecycle/lifecycle/EventCompletionService.java`
- Create: `.../eventlifecycle/lifecycle/EventCompletionJob.java`: `@Scheduled(fixedDelayString = "${eventlifecycle.completion.interval:PT1M}")`, `@ConditionalOnProperty(name = "eventlifecycle.completion.enabled", matchIfMissing = true)`
- Modify: `.../eventlifecycle/shared/EventLifecycleConfiguration.java`: add `@EnableScheduling`
- Modify: `backend/src/test/resources/application.properties`: `eventlifecycle.completion.enabled=false`
- Modify: `.../shared/persistence/LifecycleEventRepository.java`: `@Query` method `List<LifecycleEvent> findPublishedWithActivities()` (fetch join, state PUBLISHED, distinct)
- Test: `.../lifecycle/EventCompletionServiceTest.java` (`@SpringBootTest`, fixed `Clock`)

**Interfaces:**
- Produces: `EventCompletionService.completeEndedEvents(): int` (`@Transactional`; returns how many events completed). It completes each PUBLISHED event whose `lastScheduledActivityEnd()` is present and `<= now`. Events with no scheduled activity are skipped (BR-04: their cancellation is handled in Task 3). The job calls it and logs the count when it is greater than 0.

- [ ] **Step 1: Write failing tests**
  - `us23_eventCompletesWhenLastScheduledActivityEnds`: clock exactly at the last `endsAt` → COMPLETED, `completedAt == now`; returns 1.
  - `us23_eventWithFutureActivityStaysPublished`: one activity is still running → PUBLISHED; returns 0.
  - `br04_cancelledLaterActivityDoesNotDelayCompletion`: activities ending at 10:00 (scheduled) and 12:00 (cancelled), clock 10:30 → COMPLETED.
  - `br01_draftAndCancelledEventsAreNeverCompleted`: past-dated draft and cancelled events → unchanged.
  - `us23_runningTwiceIsIdempotent`: the second call returns 0.
  - `us23_completedEventIsReadOnly`: after completion, `EventReadOnlyGuard.requireEditable` → 409 `EVENT_READ_ONLY`.
- [ ] **Step 2: Run** `bash backend/mvnw -f backend/pom.xml test -Dtest=EventCompletionServiceTest`. Expected: compilation failure.
- [ ] **Step 3: Implement the service, job, query and configuration.**
- [ ] **Step 4: Run** `bash backend/mvnw -f backend/pom.xml test`. Expected: all PASS.
- [ ] **Step 5: Add the contract section "Scheduled jobs" (properties, semantics, idempotency).**
- [ ] **Step 6: Commit** `feat(lifecycle): complete events when their last activity ends (#26)`

---

### Task 6: Browse, search and filter events (US-24 #28, US-25 #29; BR-03, BR-18)

**Files:**
- Create: `.../eventlifecycle/search/EventSearchCriteria.java`: `record EventSearchCriteria(String keyword, Instant from, Instant to, UUID organizationId, EventCategory category, int page, int size)`. Compact constructor: trim the keyword, blank → null; `page >= 0`; `1 <= size <= 100`, default 20; `from` after `to` → `IllegalArgumentException`, mapped to 400 `VALIDATION_FAILED`.
- Create: `.../eventlifecycle/search/EventSearchMatcher.java`: pure ranking and filtering logic
- Create: `.../eventlifecycle/search/EventSearchService.java`
- Create: `.../eventlifecycle/search/DiscoveryController.java`
- Create: `.../eventlifecycle/search/EventSearchResult.java`: `record EventSearchResult(List<EventSummary> items, int page, int size, long totalItems, List<OrganizationOption> organizations)`, `record EventSummary(UUID id, String title, String description, EventCategory category, UUID organizationId, String organizationName, Instant nextActivityStartsAt)`, `record OrganizationOption(UUID id, String name)`
- Modify: `LifecycleEventRepository`: `@Query List<LifecycleEvent> findPublishedWithActivityEndingAfter(Instant now)` (fetch join activities, state PUBLISHED, some SCHEDULED activity with `endsAt > now`)
- Test: `.../search/EventSearchMatcherTest.java` (unit), `.../search/EventSearchServiceTest.java` (`@SpringBootTest`), `.../search/DiscoveryControllerTest.java` (`@WebMvcTest`)

**Interfaces:**
- Consumes: `EventAccessPolicy.canSeeActivity`, `OrganizationDirectory.organizationName` / `organizationIdsWithNameContaining`, `Clock`.
- Produces:
  - `EventSearchMatcher.qualifyingActivities(LifecycleEvent e, Instant now, Predicate<LifecycleActivity> visible): List<LifecycleActivity>`: SCHEDULED, `endsAt > now`, visible.
  - `EventSearchMatcher.matches(LifecycleEvent e, List<LifecycleActivity> qualifying, EventSearchCriteria c, Set<UUID> orgIdsMatchingKeyword): boolean`.
  - `EventSearchService.search(EventSearchCriteria c, Optional<UUID> userId): EventSearchResult`: `organizations` lists the distinct organizations of every event visible to the viewer, before the organization and keyword filters are applied, sorted by name. It feeds the filter dropdown.
  - HTTP: `GET /api/discovery/events?q=&from=&to=&organizationId=&category=&page=&size=` → 200 `EventSearchResult`. An unknown `category` → 400.

- [ ] **Step 1: Write failing matcher unit tests**
  - `us24_onlyUpcomingScheduledVisibleActivitiesQualify`: ended, cancelled and invisible activities are excluded.
  - `us24_keywordIsTrimmedCaseInsensitiveAndLiteral`: title "Intro to 100% Java_Basics"; keyword `"  100% java_b "` matches; `"100x"` does not; matching on the description and on the organization id set also works.
  - `us25_dateRangeMatchesOverlappingActivity`: activity 10:00–12:00; `[11:00, 13:00]` matches; `[12:30, 13:00]` does not; open-ended bounds work.
  - `us25_filtersCombineWithKeyword`: category plus organization plus keyword: all must hold.
- [ ] **Step 2: Run** `bash backend/mvnw -f backend/pom.xml test -Dtest=EventSearchMatcherTest`. Expected: compilation failure.
- [ ] **Step 3: Implement `EventSearchCriteria` and `EventSearchMatcher`; run again.** Expected: PASS.
- [ ] **Step 4: Write failing service tests**
  - `us24_listsUpcomingPublishedEventsSoonestFirst`: three published events starting in 3d, 1d and 2d → order 1d, 2d, 3d; a completed event and a past event are absent.
  - `br03_draftEventsNeverAppear`: a draft is absent, even for its Org Admin.
  - `br18_privateOnlyEventHiddenFromGuestsVisibleToMembers`: guest → absent; member (`isMember` true) → present; invitee → present.
  - `br18_privateActivitiesDoNotAffectSortOrDateFilterForGuests`: an event with a private activity tomorrow and a public one in 5 days → for a guest `nextActivityStartsAt` is the public start, and a date filter covering only tomorrow excludes the event.
  - `us24_keywordMatchesOrganizationName`: `organizationIdsWithNameContaining("robotics")` returns org X → the events of X are found.
  - `us25_paginationReportsTotal`: 25 events, page 1, size 10 → 10 items, `totalItems` 25.
  - `us25_organizationOptionsIgnoreOrganizationFilter`: filtered by org X → `organizations` still contains X and Y.
- [ ] **Step 5: Implement the service and the repository query; run** `bash backend/mvnw -f backend/pom.xml test -Dtest=EventSearchServiceTest`. Expected: PASS.
- [ ] **Step 6: Write failing controller tests and implement `DiscoveryController`**
  - `us25_queryParametersBindToCriteria`: an `ArgumentCaptor` checks keyword, ISO instants, the category enum and organizationId.
  - `us25_unknownCategoryReturns400` → 400, code `VALIDATION_FAILED`.
  - `us24_guestRequestWithoutHeaderIsAllowed` → 200.
- [ ] **Step 7: Run** `bash backend/mvnw -f backend/pom.xml test`. Expected: all PASS.
- [ ] **Step 8: Add the contract endpoint, the search assumptions and the in-memory limitation.**
- [ ] **Step 9: Commit** `feat(search): browse, search and filter published events (#28 #29)`

---

### Task 7: Give feedback (US-47 #54; BR-16)

**Files:**
- Create: `backend/src/main/resources/db/migration/eventlifecycle/V2026111002__lifecycle_create_feedback.sql`: `lifecycle_feedback(id uuid pk, activity_id uuid not null references lifecycle_activity(id) on delete cascade, author_id uuid not null, rating smallint not null check (rating between 1 and 5), comment varchar(2000) null, created_at timestamptz not null, unique (activity_id, author_id))`
- Create in `.../eventlifecycle/feedback/`: `Feedback.java` (entity), `FeedbackRepository.java`, `FeedbackEligibility.java` (pure), `FeedbackService.java`, `FeedbackController.java`, `GiveFeedbackRequest.java` (`record(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 2000) String comment)`), `FeedbackEligibilityView.java` (`record(boolean eligible, String reason)`)
- Test: `.../feedback/FeedbackEligibilityTest.java` (unit), `.../feedback/FeedbackServiceTest.java` (`@SpringBootTest`), `.../feedback/FeedbackControllerTest.java` (`@WebMvcTest`)

**Interfaces:**
- Consumes: `SignupPort.wasMarkedPresent`, `LifecycleActivityRepository`, `EventAccessPolicy.requireUser`, `Clock`.
- Produces:
  - `enum FeedbackRefusal { ACTIVITY_CANCELLED, FEEDBACK_NOT_OPEN, FEEDBACK_WINDOW_CLOSED, NOT_ATTENDED, FEEDBACK_ALREADY_GIVEN }`
  - `FeedbackEligibility.check(LifecycleActivity a, Instant now, boolean markedPresent, boolean alreadyGiven): Optional<FeedbackRefusal>`, evaluated in that enum order.
  - `FeedbackService.eligibility(UUID activityId, Optional<UUID> userId): FeedbackEligibilityView`
  - `FeedbackService.give(UUID activityId, Optional<UUID> userId, GiveFeedbackRequest req): UUID`: 401 without a user; 404 `ACTIVITY_NOT_FOUND`; a refusal → `NOT_ATTENDED` 403, the others 409, with the enum name as code. A blank comment is stored as null and other comments are trimmed. A `DataIntegrityViolationException` on the unique constraint → 409 `FEEDBACK_ALREADY_GIVEN` (concurrent double submit).
  - HTTP, base `/api/feedback`: `GET /activities/{activityId}/eligibility` → 200 view; `POST /activities/{activityId}` with body `{rating, comment}` → 201 `{ "id": "…" }`.

- [ ] **Step 1: Write failing `FeedbackEligibilityTest`**
  - `br16_feedbackWindowBoundaries`: `endsAt - 1s` → `FEEDBACK_NOT_OPEN`; `endsAt` → empty; `endsAt + 7d` → empty; `endsAt + 7d + 1s` → `FEEDBACK_WINDOW_CLOSED`.
  - `us47_participantNotMarkedPresentIsRefused` → `NOT_ATTENDED`.
  - `us47_secondFeedbackIsRefused` → `FEEDBACK_ALREADY_GIVEN`.
  - `us47_cancelledActivityRefusesFeedback` → `ACTIVITY_CANCELLED`.
- [ ] **Step 2: Run** `bash backend/mvnw -f backend/pom.xml test -Dtest=FeedbackEligibilityTest`. Expected: compilation failure. **Step 3:** implement; run again. Expected: PASS.
- [ ] **Step 4: Write failing service tests**
  - `us47_attendeeGivesRatingWithOptionalComment`: `wasMarkedPresent` → true; rating 4, comment "  Great  " → stored with comment "Great"; rating 5 with a null comment → stored.
  - `us47_onlyOncePerActivity`: second call → 409 `FEEDBACK_ALREADY_GIVEN`.
  - `br16_feedbackAllowedOnCompletedEvent`: the event is COMPLETED and within the window → accepted.
  - `us47_eligibilityEndpointReportsReason`: not present → `eligible=false`, reason `NOT_ATTENDED`.
  - `us47_guestCannotGiveFeedback` → 401.
- [ ] **Step 5: Implement the entity, repository, migration and service; run** `bash backend/mvnw -f backend/pom.xml test -Dtest=FeedbackServiceTest`. Expected: PASS.
- [ ] **Step 6: Write failing controller tests and implement `FeedbackController`**
  - `us47_ratingOutsideOneToFiveReturns400` (0 and 6), `us47_commentLongerThan2000Returns400`, `us47_validFeedbackReturns201`.
- [ ] **Step 7: Run** `bash backend/mvnw -f backend/pom.xml test`. Expected: all PASS.
- [ ] **Step 8: Add the contract table `lifecycle_feedback` (the author stored only for uniqueness), the endpoints, and the dependency on `SignupPort.wasMarkedPresent`.**
- [ ] **Step 9: Commit** `feat(feedback): attendees rate activities within seven days (#54)`

---

### Task 8: View feedback (US-48 #55; BR-17)

**Files:**
- Create: `.../eventlifecycle/feedback/FeedbackSummary.java`: `record FeedbackSummary(UUID activityId, String activityTitle, int count, Double averageRating, Map<Integer,Integer> distribution, List<FeedbackComment> comments)`, `record FeedbackComment(int rating, String comment)`
- Create: `.../eventlifecycle/feedback/FeedbackSummaryCalculator.java` (pure)
- Modify: `FeedbackService` (add `summary`), `FeedbackRepository` (`List<Feedback> findByActivityIdOrderByCreatedAtDesc(UUID)`), `FeedbackController` (`GET /api/feedback/activities/{activityId}/summary`)
- Test: `.../feedback/FeedbackSummaryCalculatorTest.java`, plus cases in `FeedbackServiceTest` and `FeedbackControllerTest`

**Interfaces:**
- Produces:
  - `FeedbackSummaryCalculator.summarize(UUID activityId, String title, List<Feedback> feedback): FeedbackSummary`: `distribution` always has keys 1–5 (zero-filled, in a `LinkedHashMap`); `averageRating` is rounded HALF_UP to 2 decimals, or null when `count == 0`; `comments` contains only entries with a non-null comment, newest first.
  - `FeedbackService.summary(UUID activityId, Optional<UUID> userId): FeedbackSummary`: 401, 404, and 403 `TEAM_MEMBER_REQUIRED` for a non-team user (Staff allowed). The report page (Task 12) calls this endpoint per activity.

- [ ] **Step 1: Write failing tests**
  - `us48_averageAndDistribution`: ratings 5, 4, 4, 1 → average 3.5, distribution `{1:1,2:0,3:0,4:2,5:1}`.
  - `us48_noFeedbackGivesNullAverageAndZeroDistribution`.
  - `br17_summaryNeverExposesAuthor`: the serialized JSON (via `JsonMapper` in the controller test) does not contain the author UUID string, nor the keys `author`, `authorId` or `userId`.
  - `us48_staffCanViewFeedback`: STAFF → 200; `us48_nonTeamUserIsRefused` → 403.
- [ ] **Step 2: Run** `bash backend/mvnw -f backend/pom.xml test -Dtest='FeedbackSummaryCalculatorTest,FeedbackServiceTest,FeedbackControllerTest'`. Expected: FAIL.
- [ ] **Step 3: Implement. Step 4: Run** `bash backend/mvnw -f backend/pom.xml test`. Expected: all PASS.
- [ ] **Step 5: Add the contract endpoint and its anonymity guarantee.**
- [ ] **Step 6: Commit** `feat(feedback): event team views anonymous feedback summaries (#55)`

---

### Task 9: Event report and CSV export (US-49 #56, US-50 #57)

**Files:**
- Create in `.../eventlifecycle/reports/`: `EventReport.java` (`record EventReport(UUID eventId, String eventTitle, EventState eventState, List<ActivityReportRow> activities)`, `record ActivityReportRow(UUID activityId, String title, Instant startsAt, ActivityState state, int confirmedSignups, int waitlistSize, int cancellations, Double attendanceRate, Double averageRating, int ratingCount)`), `ReportCalculator.java` (pure), `EventReportService.java`, `ReportCsvWriter.java` (pure), `ReportController.java`
- Modify: `FeedbackRepository`: add an aggregate query `List<RatingAggregate> aggregateByActivityIds(Collection<UUID> ids)` (a projection with `activityId`, `count`, `average`)
- Test: `.../reports/ReportCalculatorTest.java`, `.../reports/ReportCsvWriterTest.java`, `.../reports/EventReportServiceTest.java` (`@SpringBootTest`), `.../reports/ReportControllerTest.java` (`@WebMvcTest`)

**Interfaces:**
- Consumes: `SignupPort.figures`, the feedback aggregates, `EventAccessPolicy`.
- Produces:
  - `ReportCalculator.row(LifecycleActivity a, SignupFigures f, long ratingCount, Double average): ActivityReportRow`: `confirmedSignups = f.confirmed() + f.attended()`; `attendanceRate = attended / (confirmed + attended)`, or null when that is 0; the average is rounded HALF_UP to 2 decimals.
  - `EventReportService.report(UUID eventId, Optional<UUID> userId): EventReport`: 401, 404, 403 `TEAM_MEMBER_REQUIRED`. Rows are ordered by `startsAt` (nulls last), then title. Every event state is allowed.
  - `ReportCsvWriter.write(EventReport r): String`. Header, exactly: `activity_title,starts_at,status,confirmed_signups,waitlist_size,cancellations,attendance_rate_percent,average_rating,rating_count`. One row per activity; `starts_at` in ISO-8601 UTC; percent with one decimal (`66.7`); null → empty cell; line ending `\r\n`. Cells are quoted when they contain `,`, `"`, CR or LF, with `"` doubled. A text cell starting with `=`, `+`, `-` or `@` is prefixed with `'`.
  - HTTP, base `/api/reports`: `GET /events/{eventId}` → 200 JSON `EventReport`; `GET /events/{eventId}/csv` → 200 `text/csv; charset=UTF-8` with `Content-Disposition: attachment; filename="event-<eventId>-report.csv"`.

- [ ] **Step 1: Write failing pure tests**
  - `us49_attendanceRateIsShareOfConfirmedMarkedAttended`: figures (confirmed 1, waitlisted 2, cancelled 3, attended 3) → confirmedSignups 4, waitlistSize 2, cancellations 3, attendanceRate 0.75.
  - `us49_noConfirmedParticipantsGivesNullRate`.
  - `us50_csvHasOneRowPerActivity`: 3 activities → 4 lines, and the header equals the exact string above.
  - `us50_csvEscapesAndNeutralisesFormulas`: title `=SUM(A1)` → `'=SUM(A1)`; title `Talk, "AI"\nDay` → `"Talk, ""AI""\nDay"`.
  - `us50_csvContainsAggregatesOnly`: the CSV text does not contain any UUID (regex `[0-9a-f]{8}-[0-9a-f]{4}`).
- [ ] **Step 2: Run** `bash backend/mvnw -f backend/pom.xml test -Dtest='ReportCalculatorTest,ReportCsvWriterTest'`. Expected: compilation failure. **Step 3:** implement; run again. Expected: PASS.
- [ ] **Step 4: Write failing service and controller tests**
  - `us49_reportShowsFiguresPerActivity`: `SignupPort.figures` is mocked per activity and real feedback rows are persisted → the rows match, including `averageRating`.
  - `us49_staffCanViewReport` and `us49_nonTeamUserIsRefused` (403).
  - `us50_csvEndpointDownloadsAttachment`: the content type starts with `text/csv` and the `Content-Disposition` header contains `attachment` and `.csv`.
- [ ] **Step 5: Implement the service, query and controller; run** `bash backend/mvnw -f backend/pom.xml test`. Expected: all PASS.
- [ ] **Step 6: Add the contract endpoints, the CSV format, the formula assumption and the dependency on `SignupPort.figures`.**
- [ ] **Step 7: Commit** `feat(reports): event report with CSV export (#56 #57)`

---

### Task 10: Frontend scaffold and search page (US-24 #28, US-25 #29)

**Files:**
- Create the `frontend/` scaffold (a shared setup file; list it in the contract): run `npm create vite@latest frontend -- --template react-ts`, then install `react-router` (v7), `tailwindcss` and `@tailwindcss/vite`, plus dev dependencies `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/user-event` and `@testing-library/jest-dom`. Pin the exact installed versions in `package.json` (no `^`). `vite.config.ts`: the react and tailwind plugins, `server.proxy['/api'] = 'http://localhost:8080'`, `test: { environment: 'jsdom', setupFiles: './src/test/setup.ts', globals: true }`. `package.json` scripts: `dev`, `build`, `test` (`vitest run`). Keep `App.tsx` minimal: a `BrowserRouter` that mounts `eventLifecycleRoutes`.
- Create `frontend/src/features/event-lifecycle/`:
  - `api/client.ts`: `apiFetch<T>(path: string, init?: RequestInit): Promise<T>`. Adds `X-User-Id` from `localStorage.getItem('devUserId')` when it is present (provisional). A non-2xx response throws `ApiError { status: number; code: string; message: string }` built from the ProblemDetail.
  - `api/types.ts`: TypeScript mirrors of every backend record in Tasks 3–9 (same field names).
  - `api/discovery.ts`: `searchEvents(params: SearchParams): Promise<EventSearchResult>`, with `SearchParams { q?: string; from?: string; to?: string; organizationId?: string; category?: EventCategory; page?: number }`. It omits empty params.
  - `search/EventSearchPage.tsx`: a keyword input, from/to `<input type="date">` (converted to start-of-day and end-of-day instants in the browser's time zone), an organization `<select>` filled from `result.organizations`, a category `<select>` with labels Workshop … Other, and a results list (title, organization, category label, next start date). The empty state reads "No events match your search." Filters are mirrored into URL search params.
  - `routes.tsx`: `export const eventLifecycleRoutes: RouteObject[]` with `/events` mapped to `EventSearchPage`.
  - `categories.ts`: `CATEGORY_LABELS: Record<EventCategory, string>`.
- Test: `frontend/src/features/event-lifecycle/search/EventSearchPage.test.tsx` (`vi.stubGlobal('fetch', …)`)

- [ ] **Step 1: Scaffold and confirm** `cd frontend && npm test -- --passWithNoTests` exits 0 and `npm run build` succeeds.
- [ ] **Step 2: Write failing tests**
  - `US-24 lists events in the order returned and shows organization and category`.
  - `US-24 shows the empty state when nothing matches`.
  - `US-24 sends the trimmed keyword as q`: typing `"  java "` and submitting calls fetch with a URL containing `q=java`.
  - `US-25 offers the six categories with their labels`.
  - `US-25 combines date range, organization and category with the keyword`: the fetch URL contains all of `from=`, `to=`, `organizationId=`, `category=WORKSHOP` and `q=`.
  - `US-24 shows the API error message on failure`.
- [ ] **Step 3: Run** `cd frontend && npm test`. Expected: FAIL.
- [ ] **Step 4: Implement; run** `npm test` and `npm run build`. Expected: PASS and a clean build.
- [ ] **Step 5: Add a contract section "Frontend" (folder, routes, provisional `devUserId` header) and list the scaffold under shared files.**
- [ ] **Step 6: Commit** `feat(frontend): scaffold SPA and add event search page (#28 #29)`

---

### Task 11: Frontend lifecycle management page (US-20 #23, US-21 #24, US-22 #25)

**Files:**
- Create: `frontend/src/features/event-lifecycle/api/lifecycle.ts`: `getLifecycleEvent(id)`, `cancelEvent(id)`, `cancelActivity(activityId)`, `deleteDraft(id)`
- Create: `frontend/src/features/event-lifecycle/lifecycle/EventLifecyclePage.tsx`, route `/events/:eventId/lifecycle`
- Modify: `routes.tsx`
- Test: `frontend/src/features/event-lifecycle/lifecycle/EventLifecyclePage.test.tsx`

Behaviour: the page shows the event state and an activity list with each activity's state. The buttons "Cancel event", "Cancel activity" (per scheduled activity) and "Delete draft" render only when `permissions.canCancel` / `canDelete` allow them. Every action asks first with `window.confirm`; the confirmation text names the cascade ("All signups and waitlist entries will be cancelled."). After a cancel the page re-renders from the returned view; after a delete it navigates to `/events`. An `ApiError` message is shown in an alert region (`role="alert"`). A read-only event (COMPLETED or CANCELLED) shows "This event is read-only." and no actions.

- [ ] **Step 1: Write failing tests**
  - `US-20 cancelling an activity confirms the cascade and shows it cancelled`.
  - `US-20 hides cancel actions without owner rights`.
  - `US-21 cancelling the event shows the cancelled read-only state`.
  - `US-22 delete is offered only for drafts and navigates to the search page`.
  - `BR-06 a completed event shows no actions`.
  - `US-20 shows the server refusal message`.
- [ ] **Step 2: Run** `cd frontend && npm test`. Expected: FAIL. **Step 3:** implement; run `npm test` and `npm run build`. Expected: PASS.
- [ ] **Step 4: Update the contract frontend routes.**
- [ ] **Step 5: Commit** `feat(frontend): owner cancels events and activities, deletes drafts (#23 #24 #25)`

---

### Task 12: Frontend feedback and report pages (US-47 #54, US-48 #55, US-49 #56, US-50 #57)

**Files:**
- Create: `frontend/src/features/event-lifecycle/api/feedback.ts` (`getEligibility`, `giveFeedback`, `getSummary`) and `api/reports.ts` (`getReport`, `reportCsvUrl(eventId)`)
- Create: `frontend/src/features/event-lifecycle/feedback/GiveFeedbackPage.tsx`, route `/activities/:activityId/feedback`
- Create: `frontend/src/features/event-lifecycle/reports/EventReportPage.tsx`, route `/events/:eventId/report`
- Test: `feedback/GiveFeedbackPage.test.tsx`, `reports/EventReportPage.test.tsx`

Behaviour:
- `GiveFeedbackPage`: loads the eligibility. When the user is not eligible it shows the message for the reason (`NOT_ATTENDED`: "Only participants marked present can give feedback."; `FEEDBACK_NOT_OPEN`: "Feedback opens when the activity ends."; `FEEDBACK_WINDOW_CLOSED`: "Feedback closed 7 days after the activity ended."; `FEEDBACK_ALREADY_GIVEN`: "You have already rated this activity."; `ACTIVITY_CANCELLED`: "This activity was cancelled."). Otherwise it shows a 1–5 radio group (required), an optional comment textarea (maxLength 2000) and a submit button; on success it shows "Thank you for your feedback."
- `EventReportPage`: a table with one row per activity (title, start, status, confirmed, waitlist, cancellations, attendance %, average rating), where a null value shows "—". A "Download CSV" link points to `/api/reports/events/{id}/csv` (`download` attribute). Each row expands a feedback panel that loads the summary and shows the distribution (5→1 bars with counts) and the comments, with no author shown.

- [ ] **Step 1: Write failing tests**
  - `US-47 eligible attendee submits a rating and comment`: the POST body equals `{"rating":4,"comment":"Nice"}`; the thank-you text is shown.
  - `US-47 rating is required before submitting` (no fetch POST is made).
  - `BR-16 ineligible reasons show their messages` (table-driven over the 5 reasons).
  - `US-48 feedback panel shows average, distribution and comments without authors`.
  - `US-49 report table shows figures and a dash for missing rates`.
  - `US-50 offers the CSV download link for the event`.
- [ ] **Step 2: Run** `cd frontend && npm test`. Expected: FAIL. **Step 3:** implement; run `npm test` and `npm run build`. Expected: PASS.
- [ ] **Step 4: Update the contract frontend routes.**
- [ ] **Step 5: Commit** `feat(frontend): feedback form, feedback summaries and event report (#54 #55 #56 #57)`

---

### Task 13: Contract completion, GenAI record, verification, push and PR

**Files:**
- Modify: `docs/integration/event-lifecycle-contract.md`: final pass. Check that it covers every section: shared files changed; provisional model (fields, types, constraints, states); tables and migrations; endpoints with request/response shapes and error codes; provided interfaces (`EventReadOnlyGuard`); stubbed interfaces; assumptions about other shares; time and formula assumptions; frontend routes; known limitations; and a reconciliation checklist (which stub replaces which real bean, which provisional entity maps to which authoring entity).
- Create: `docs/genai-usage/entries/<UTC>-issue-23-duartelagoela-<uuid4>.md` from the template: model "Claude Opus 5.5 (Claude Code, VS Code extension)", effort "Not confirmed for this task", all ten issues, only the commands actually run with their results, human review "Pending student review". Do not edit `docs/genai_usage_log.md` (the integrator consolidates it).

- [ ] **Step 1: Run** `bash backend/mvnw -f backend/pom.xml verify`. Expected: BUILD SUCCESS. Record the test counts.
- [ ] **Step 2: Run** `cd frontend && npm test && npm run build`. Expected: PASS and a clean build.
- [ ] **Step 3: Run** `git diff --name-only origin/main...HEAD`. Every path is inside the allowed list from Global Constraints.
- [ ] **Step 4: Finish the contract and write the GenAI entry; commit** `docs: complete event-lifecycle integration contract and GenAI record (#23 #24 #25 #26 #28 #29 #54 #55 #56 #57)`
- [ ] **Step 5: Push** `git push -u origin feature/event-lifecycle`.
- [ ] **Step 6: Open the PR** with `gh pr create -R neon1024/gen-ai-in-swe-project --base main --head feature/event-lifecycle`. Title: `Event lifecycle, search, feedback and reporting (DuarteLagoela share)`. The body has a summary, a link to `docs/integration/event-lifecycle-contract.md`, the verification commands and results, a "Not done / limitations" section, one `Closes #N` line for each of 23, 24, 25, 26, 28, 29, 54, 55, 56 and 57 (completed ones only), and the attribution footer. Do not merge it.
