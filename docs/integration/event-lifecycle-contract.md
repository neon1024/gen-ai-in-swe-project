# Event Lifecycle Integration Contract

## Purpose

This document lists everything the event-lifecycle share (`com.ubb.genai.eventlifecycle`: event cancellation, search, feedback, reports) adds to the repository. It is intended for the agent that reconciles this share with the event-authoring share (jose03pedro).

## Shared files changed

| File | Change |
|---|---|
| `backend/pom.xml` | Enabled `spring-boot-starter-data-jpa` and `spring-boot-starter-data-jpa-test`. Added `spring-boot-starter-flyway`, `flyway-database-postgresql`, `postgresql` (runtime), and test-scope `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-junit-jupiter`. |
| `backend/src/main/resources/application.properties` | Added datasource (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` with local defaults), `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`. |
| `backend/src/test/resources/application.properties` | New. Testcontainers JDBC URL `jdbc:tc:postgresql:17-alpine:///ubb_events_test`, `ddl-auto=validate`, `open-in-view=false`. |

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
