# Event Lifecycle Integration Contract

## Purpose

This document lists everything the event-lifecycle share (`com.ubb.genai.eventlifecycle`: event cancellation, search, feedback, reports) adds to the repository. It is intended for the agent that reconciles this share with the event-authoring share (jose03pedro).

## Shared files changed

| File | Change |
|---|---|
| `backend/pom.xml` | Enabled `spring-boot-starter-data-jpa` and `spring-boot-starter-data-jpa-test`. Added `spring-boot-starter-flyway`, `flyway-database-postgresql`, `postgresql` (runtime), and test-scope `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-junit-jupiter`. |
| `backend/src/main/resources/application.properties` | Added datasource (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` with local defaults), `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`. |
| `backend/src/test/resources/application.properties` | New. Testcontainers JDBC URL `jdbc:tc:postgresql:17-alpine:///ubb_events_test`, `ddl-auto=validate`, `open-in-view=false`, `eventlifecycle.completion.enabled=false`. |

## Provisional model

Package `com.ubb.genai.eventlifecycle.shared.model`. Every class is marked provisional in its Javadoc and is replaced at reconciliation by the event-authoring model.

| Class | Table | Notes |
|---|---|---|
| `LifecycleEvent` | `lifecycle_event` | `@Version long version`; `activities` one-to-many (cascade ALL, orphan removal). Transitions: `publish` (DRAFT to PUBLISHED, fixtures only), `cancel` (DRAFT or PUBLISHED to CANCELLED, cancels SCHEDULED activities), `complete` (PUBLISHED to COMPLETED). Queries: `isReadOnly`, `allActivitiesCancelled`, `lastScheduledActivityEnd`. |
| `LifecycleActivity` | `lifecycle_activity` | `cancel(now)`, `isCancelled`, `hasEnded(now)` (`endsAt != null && now >= endsAt`), `getEventId`. |
| `LifecycleAgendaItem` | `lifecycle_agenda_item` | Registers itself on its activity at construction. |
| `EventState` | | `DRAFT, PUBLISHED, COMPLETED, CANCELLED` |
| `ActivityState` | | `SCHEDULED, CANCELLED` |
| `ActivityVisibility` | | `PUBLIC, PRIVATE` |
| `EventCategory` | | `WORKSHOP, CONFERENCE, TRAINING, SOCIAL, COMPETITION, OTHER` |
| `IllegalEventTransitionException` | | Unchecked; `code()` is one of `EVENT_NOT_PUBLISHABLE`, `EVENT_NOT_CANCELLABLE`, `EVENT_NOT_COMPLETABLE`, `ACTIVITY_ALREADY_CANCELLED`. |

Repositories (`shared.persistence`): `LifecycleEventRepository`, `LifecycleActivityRepository` (both `JpaRepository<_, UUID>`).

`shared.EventLifecycleConfiguration` provides a `Clock` bean (`Clock.systemUTC()`, `@ConditionalOnMissingBean`). All time-dependent logic uses the injected `Clock`.

## Tables and migrations

Migrations live in `backend/src/main/resources/db/migration/eventlifecycle/` and use timestamp versions.

| Migration | Creates |
|---|---|
| `V2026111001__lifecycle_create_event_activity_agenda.sql` | `lifecycle_event`, `lifecycle_activity`, `lifecycle_agenda_item` |

| Table | Key columns and constraints |
|---|---|
| `lifecycle_event` | `id uuid pk`, `organization_id uuid`, `title varchar(200)`, `description text`, `category` (check: 6 values), `state` (check: 4 values), `published_at`, `cancelled_at`, `completed_at` (`timestamptz`, nullable), `version bigint default 0`. Index on `state`. |
| `lifecycle_activity` | `id uuid pk`, `event_id` FK to `lifecycle_event` (cascade delete), `title`, `description text default ''`, `starts_at`, `ends_at`, `location`, `online_link`, `capacity` (check >= 1), `registration_opens_at`, `registration_closes_at`, `visibility` (check), `state` (check), `cancelled_at`. Check `ends_at > starts_at` when both set. Index on `event_id`. |
| `lifecycle_agenda_item` | `id uuid pk`, `activity_id` FK to `lifecycle_activity` (cascade delete), `starts_at timestamptz`, `title`, `speaker`. |

## Assumptions

| Topic | Decision |
|---|---|
| Time boundaries | An activity has ended when `now >= endsAt`. An activity is cancellable while `now < endsAt`. |
| Instants | Stored as `timestamptz`, handled as `java.time.Instant` (UTC). |
| Identifiers | `UUID` for events, activities and agenda items. |
| Flyway | Default `classpath:db/migration` location; Flyway scans subfolders, so each share keeps its own subfolder. |

## Interfaces stubbed

Ports live in `shared.port`; each has a `@Component` stub in `shared.port.stub` whose Javadoc names the owning share. A real implementation replaces the stub bean at reconciliation.

| Port | Method | Semantics | Owning share | Stub behaviour |
|---|---|---|---|---|
| `CurrentUserProvider` | `Optional<UUID> currentUserId()` | Caller of the current request; empty for a guest. | neon1024 | `HeaderCurrentUserProvider`: reads `X-User-Id` through `RequestContextHolder`; empty when missing or not a UUID. |
| `OrganizationDirectory` | `boolean isMember(orgId, userId)` | User belongs to the organization. | All16 | `false` |
| | `boolean isOrgAdmin(orgId, userId)` | User is an Org Admin of the organization. | All16 | `false` |
| | `Optional<String> organizationName(orgId)` | Display name of the organization. | All16 | empty |
| | `Set<UUID> organizationIdsWithNameContaining(keyword)` | Organizations whose name contains the keyword, case-insensitive. | All16 | empty set |
| `EventTeamDirectory` | `Optional<EventRole> roleOf(eventId, userId)` | Event-team role (`OWNER`, `EDITOR`, `STAFF`). | All16 | empty |
| `PrivateActivityInvitations` | `boolean isInvited(activityId, userId)` | User holds an invitation to a private activity. | jose03pedro | `false` |
| `SignupPort` | `Set<UUID> cancelAllSignups(activityId)` | Cancels all signups; returns users who held a Confirmed or Waitlisted signup. | lucianvaleanu | empty set |
| | `SignupFigures figures(activityId)` | `confirmed`, `waitlisted`, `cancelled`, `attended` counts. | lucianvaleanu | all zero |
| | `boolean wasMarkedPresent(activityId, userId)` | User was marked present at the activity. | lucianvaleanu | `false` |
| `NotificationPort` | `void activityCancelled(activityId, recipients)` | Notify users of an activity cancellation. | neon1024 | logs ids at INFO |
| | `void eventCancelled(eventId, recipients)` | Notify users of an event cancellation. | neon1024 | logs ids at INFO |

## Authorization assumptions

| Topic | Decision |
|---|---|
| Current user | Taken from the `X-User-Id` header (UUID). A missing or malformed header means a guest. |
| Owner rights | Event role `OWNER`, or Org Admin of the event's organization. |
| Team member | Any event role (`OWNER`, `EDITOR`, `STAFF`), or Org Admin of the event's organization. |
| Draft visibility (BR-03) | A DRAFT event is visible only to team members; anyone else gets 404, never 403. |
| Private activities (BR-18) | Visible when PUBLIC, or when the user is a member of the event's organization or is invited. |

`shared.access.EventAccessPolicy` implements these rules: `hasOwnerRights`, `isTeamMember`, `canSeeEvent`, `canSeeActivity`, `requireUser` (401 `UNAUTHENTICATED`), `requireVisibleEvent` (404 `EVENT_NOT_FOUND`), `requireOwnerRights` (403 `OWNER_RIGHTS_REQUIRED`), `requireTeamMember` (403 `TEAM_MEMBER_REQUIRED`).

## Error format

Errors are RFC 9457 `ProblemDetail` bodies with an extra `code` property (UPPER_SNAKE), produced by `shared.web.EventLifecycleExceptionHandler` (scoped to `com.ubb.genai.eventlifecycle`).

| Status | Cause | `code` |
|---|---|---|
| 400 | Bean validation, argument type mismatch, method validation | `VALIDATION_FAILED` |
| 401 | No current user | `UNAUTHENTICATED` |
| 403 | Insufficient role | `OWNER_RIGHTS_REQUIRED`, `TEAM_MEMBER_REQUIRED` |
| 404 | Not found or hidden | `EVENT_NOT_FOUND` and other `*_NOT_FOUND` |
| 409 | `IllegalEventTransitionException` | the exception's `code()` |
| 409 | `LifecycleProblem.conflict` | caller-supplied |
| 409 | Optimistic locking failure | `CONCURRENT_MODIFICATION` |

## Endpoints

Base path `/api/lifecycle`. The caller comes from `CurrentUserProvider`; the GET and POST endpoints answer with the same view; DELETE answers 204 with no body.

| Method and path | Purpose | Success | Errors (`code`) |
|---|---|---|---|
| `GET /events/{eventId}` | Event with its visible activities and the caller's permissions | 200 | 404 `EVENT_NOT_FOUND` |
| `POST /events/{eventId}/cancel` | Cancel a DRAFT or PUBLISHED event (US-21) | 200 | 401 `UNAUTHENTICATED`, 403 `OWNER_RIGHTS_REQUIRED`, 404 `EVENT_NOT_FOUND`, 409 `EVENT_NOT_CANCELLABLE` |
| `POST /activities/{activityId}/cancel` | Cancel a scheduled activity (US-20) | 200 | 401 `UNAUTHENTICATED`, 403 `OWNER_RIGHTS_REQUIRED`, 404 `ACTIVITY_NOT_FOUND` or `EVENT_NOT_FOUND`, 409 `EVENT_READ_ONLY`, `ACTIVITY_ALREADY_CANCELLED` or `ACTIVITY_ENDED` |
| `DELETE /events/{eventId}` | Delete a DRAFT event with its activities and agenda items (US-22) | 204 | 401 `UNAUTHENTICATED`, 403 `OWNER_RIGHTS_REQUIRED`, 404 `EVENT_NOT_FOUND`, 409 `EVENT_NOT_DRAFT` |

None of the POST and DELETE endpoints has a request body. A malformed id returns 400 `VALIDATION_FAILED`.

Response (`POST /api/lifecycle/activities/6b1f.../cancel`):

```json
{
  "id": "0d5c4a52-6a0e-4a38-9f3e-2f0f1c8a7b11",
  "title": "Sample event",
  "state": "PUBLISHED",
  "activities": [
    { "id": "6b1f2c9e-3c1d-4e53-8f7a-0a5d3e9b7c21", "title": "Activity 1",
      "startsAt": "2027-02-01T10:00:00Z", "endsAt": "2027-02-01T11:00:00Z", "state": "CANCELLED" },
    { "id": "a93e7d10-5b64-4f0c-9d2b-7c1e8f4a6b32", "title": "Activity 2",
      "startsAt": "2027-02-01T11:00:00Z", "endsAt": "2027-02-01T12:00:00Z", "state": "SCHEDULED" }
  ],
  "permissions": { "canCancel": true, "canDelete": false }
}
```

Error response (`409`, `application/problem+json`):

```json
{ "type": "about:blank", "title": "Conflict", "status": 409,
  "detail": "The activity has already ended", "code": "ACTIVITY_ENDED" }
```

`permissions.canCancel` is true for callers with owner rights while the event is DRAFT or PUBLISHED. `permissions.canDelete` is true for callers with owner rights while the event is DRAFT. Callers who are not team members only see PUBLIC activities, or private ones they may see (BR-18).

## Cancellation rules

| Rule | Behaviour |
|---|---|
| Precondition order (activity) | missing activity 404, event not visible 404, no owner rights 403, event COMPLETED or CANCELLED 409 `EVENT_READ_ONLY`, activity already cancelled 409, activity ended (`now >= endsAt`) 409. |
| Precondition order (event) | event not visible 404, no owner rights 403, event COMPLETED or CANCELLED 409 `EVENT_NOT_CANCELLABLE` (BR-06). |
| Activity cancel | The activity becomes CANCELLED, `SignupPort.cancelAllSignups(activityId)` is called, then `NotificationPort.activityCancelled(activityId, recipients)`. |
| BR-04 cascade | When the cancelled activity was the last non-cancelled one, the event becomes CANCELLED in the same transaction and `NotificationPort.eventCancelled(eventId, recipients)` is also called. |
| Event cancel | `cancelAllSignups` is called once for every activity that was SCHEDULED, the event and those activities become CANCELLED, and `eventCancelled` is called once with the union of the recipients. Already cancelled activities are not touched. |
| Rejected requests | Nothing is cancelled and no notification is sent. |
| Notifications | Sent after the state change, in the same transaction, so a failing port rolls the change back. |
| Concurrency | Both cancel paths load the event with an optimistic force-increment lock, so every cancel bumps the event version and the loser of a race gets 409 `CONCURRENT_MODIFICATION`. |

## Delete rules

| Rule | Behaviour |
|---|---|
| Precondition order | event not visible 404, no owner rights 403, event not DRAFT 409 `EVENT_NOT_DRAFT` ("Published events cannot be deleted, only cancelled.", BR-05). |
| Cascade | Activities and agenda items are removed by JPA cascade and by `on delete cascade` in the database. |

## Scheduled jobs

| Job | Behaviour |
|---|---|
| `EventCompletionJob` | Runs `EventCompletionService.completeEndedEvents()` with a fixed delay. Logs the count when at least one event was completed. A failed run is logged and retried on the next tick. |
| `eventlifecycle.completion.interval` | Delay between runs as an ISO-8601 duration. Default `PT1M`. |
| `eventlifecycle.completion.enabled` | Set to `false` to disable the job. Default `true`. The test configuration disables it. |
| Selection | Every PUBLISHED event whose last non-cancelled activity has `endsAt <= now` becomes COMPLETED with `completedAt = now` (US-23, BR-04). Events with no scheduled activity are skipped. DRAFT, COMPLETED and CANCELLED events are never selected (BR-01). |
| Idempotency | A completed event is no longer selected, so a repeated run completes nothing. |
| Transaction | One transaction per run. A concurrent cancellation of a selected event fails the save, the run rolls back and the next run retries. |

## Provided interfaces

| Interface | Method | Semantics |
|---|---|---|
| `lifecycle.EventReadOnlyGuard` (Spring bean) | `void requireEditable(UUID eventId)` | 404 `EVENT_NOT_FOUND` when the event is missing; 409 `EVENT_READ_ONLY` when it is COMPLETED or CANCELLED (BR-06). |

The event-authoring share should call `requireEditable` before every edit of an event, its activities or its agenda. Attendance and feedback deliberately skip the guard (BR-06 exception).
