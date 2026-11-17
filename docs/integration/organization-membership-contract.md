# Organization membership contract

## Scope and implementation boundary

This contract covers US-06 through US-10 only. Organizations are seeded demo records; organization registration, user accounts, event-team assignment management, ownership transfer, and production authentication are outside this slice.

The browser route is `/organizations`. It uses the same-origin `/api/organizations` endpoints and the Vite development proxy to `http://localhost:8080`.

## Demo identity warning

For local demonstration, the existing `CurrentUserProvider` reads `X-User-Id`. The selector on `/organizations` writes the selected UUID to `localStorage.devUserId`; the shared API client sends that value as the header. The seeded UUIDs are demonstration identities only. This header can be forged and is **not authentication or production authorization**. Replace this seam with the approved authenticated principal before deployment; do not expose this selector in production.

Seeded demonstration identities:

| UUID suffix | Label |
|---|---|
| `...0001` | Student applicant; also a Member of Language Exchange |
| `...0002` | AI Society Org Admin |
| `...0003` | Robotics Club Org Admin |
| `...0004` | Language Exchange Org Admin |
| `...0005` | Photography Group Org Admin |
| `...0006` | AI Society co-admin |
| `...0007` | Organizer / member |
| `...0008` | Member / organizer |

The API does not expose organization contact email or demo credentials.

## HTTP API

All IDs are UUIDs. Requests without a valid demo identity return `401 AUTHENTICATION_REQUIRED`. Admin-only reads and mutations require an `ORG_ADMIN` membership and return `403 ORG_ADMIN_REQUIRED` otherwise. Missing/inactive organizations and missing members/requests return `404`; invalid transitions and business-rule conflicts return `409` with a stable `code` property and human-readable `detail`.

| Method and path | Access | Behavior |
|---|---|---|
| `GET /api/organizations` | Any identified demo user | List active organizations with `id`, `name`, `description`, `member`, `role`, and latest visible `requestStatus`. |
| `POST /api/organizations/{organizationId}/join-requests` | Any identified non-member | Create a pending request; returns `201` and request `{id, applicantId, createdAt}`. |
| `DELETE /api/organizations/{organizationId}/join-requests/me` | Applicant | Withdraw the caller's pending request; returns `204`. |
| `GET /api/organizations/{organizationId}/join-requests` | Org Admin | List pending requests in creation order. |
| `POST /api/organizations/{organizationId}/join-requests/{requestId}/approve` | Org Admin | Add applicant as `MEMBER`, close request as `APPROVED`; returns `204`. |
| `POST /api/organizations/{organizationId}/join-requests/{requestId}/reject` | Org Admin | Close request as `REJECTED`; a later request is allowed; returns `204`. |
| `GET /api/organizations/{organizationId}/members` | Org Admin | List member UUID/role and event-team/active-event-owner indicators. |
| `PATCH /api/organizations/{organizationId}/members/{userId}` | Org Admin | Set JSON body `{"role":"MEMBER|ORGANIZER|ORG_ADMIN"}`. |
| `DELETE /api/organizations/{organizationId}/members/{userId}` | Org Admin | Remove another member and their event-team assignments; returns `204`. |
| `DELETE /api/organizations/{organizationId}/members/me` | Member | Leave the organization and remove the caller's event-team assignments; returns `204`. |

Stable conflict codes include `ALREADY_MEMBER`, `REQUEST_PENDING`, `NO_PENDING_REQUEST`, `REQUEST_NOT_PENDING`, `LAST_ORG_ADMIN`, and `ACTIVE_EVENT_OWNER`.

## Persistence and rules

Flyway owns schema `organizations` and the `organization`, `organization_membership`, `join_request`, and `event_team_membership` tables. Migration versions are `V2026111610` and `V2026111611`; these follow the existing timestamped authoring and lifecycle versions and use the shared default Flyway location/history. Hibernate schema updates remain disabled; `ddl-auto=validate`.

Mutating operations lock the active organization row to serialize membership and join-request changes. Event-authoring organization checks also lock that row in write transactions, so new drafts cannot race a concurrent leave/removal. The partial unique pending-request index also prevents duplicate pending requests. `BR-21` is enforced while holding this lock, so role changes, removals, departures, and request approval cannot leave the organization without an Org Admin. Removal/departure checks `event_authoring.event` and refuses while the user owns a `DRAFT` or `PUBLISHED` event (`BR-23`). Successful removal/departure deletes all matching event-team assignments in the same transaction (`BR-22`).

The event-team table is only the persistence/cleanup seam required by US-09 and US-10. This slice does not provide event-team assignment UI or assignment-management endpoints. `EventTeamDirectory` resolves event Owner from event authoring and Editor/Staff roles from this table; future team-management work must enforce that assignees are members of the event's organization.

## Verification state

As of 2026-11-17, the frontend standalone validation passed its production build, all 43 Vitest tests, and the `/organizations` HTTP probe; the configured lint command also passed. The backend `rtk proxy bash backend/mvnw -f backend/pom.xml clean test` run passed all 315 tests, including PostgreSQL-backed MockMvc, migration, and concurrency coverage. A live local startup against an isolated PostgreSQL 17.6 database applied all five migrations and passed Hibernate schema validation; `/api/organizations` and `/organizations` both returned HTTP 200.

The demo `X-User-Id` identity remains spoofable and is not production authentication. Student review remains pending.
