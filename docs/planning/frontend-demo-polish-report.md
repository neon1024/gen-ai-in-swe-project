# Frontend Demo Polish Report

## Branch and purpose

- Branch: `frontend/demo-polish`
- Base: `main`
- Base commit: `8583110`
- Purpose: translate the newly integrated mid-term backend into a complete, reproducible frontend demo and identify the remaining frontend responsibilities.

## What is new on `main`

### Account and email verification

The application now supports the complete local account flow:

- Public `/signup` and `/login` pages.
- User registration through `POST /api/auth/register`.
- HTTP-only session login through `POST /api/auth/login`.
- Logout through `POST /api/auth/logout`.
- `/check-email` after registration.
- `/verify-email` for account activation.
- Login refusal for pending verification and deactivated accounts.
- Mailpit locally catches verification emails.

Frontend locations:

- `frontend/src/features/authentication/pages/AuthPage.tsx`
- `frontend/src/features/authentication/pages/CheckEmailPage.tsx`
- `frontend/src/features/authentication/pages/VerifyAccountPage.tsx`
- `frontend/src/features/authentication/context.tsx`
- `frontend/src/features/authentication/guards.tsx`

### Organization membership

The integrated backend now exposes organization discovery and membership administration:

- List organizations and membership roles.
- Request to join an organization.
- Withdraw a pending join request.
- Review pending join requests.
- Approve or reject a join request.
- List organization members.
- Change member roles.
- Remove a member.
- Leave an organization.

Frontend location:

- `frontend/src/features/organization-membership/OrganizationMembershipPage.tsx`
- `frontend/src/features/organization-membership/organizationMembershipService.ts`

The demo navigation now includes an **Organizations** entry.

### Event authoring

The frontend and backend now support event authoring workflows:

- List the current user's editable draft and published events.
- Create a draft event.
- Edit event details.
- Add, edit and remove activities.
- Add, edit and remove agenda items.
- Publish a draft after backend readiness validation.
- Cancel activities and events.
- Delete eligible drafts.
- Reflect optimistic version conflicts and publication violations in the UI.

Frontend locations:

- `frontend/src/features/event-authoring/MyEventsPage.tsx`
- `frontend/src/features/event-authoring/NewEventPage.tsx`
- `frontend/src/features/event-authoring/EventAuthoringPage.tsx`
- `frontend/src/features/event-authoring/DateTimeField.tsx`
- `frontend/src/features/event-authoring/api.ts`

Routes:

- `/authoring/events`
- `/authoring/events/new`
- `/authoring/events/:eventId`

### Discovery and lifecycle integration

Published authoring events are mirrored into lifecycle tables so discovery can show them. The existing frontend still provides:

- Event discovery and filtering.
- Event lifecycle actions.
- Event reports and CSV export.
- Feedback submission and feedback summaries.
- Loading, empty and error states.

The main demo currently seeds:

- Five demo accounts.
- Four organizations.
- Organization memberships.
- One pending join request.
- One published **AI Society Code Sprint** event.
- One **Code competition** activity and a complete agenda.

### Demo runtime

The repository now includes `scripts/run-app.sh`, which starts:

- PostgreSQL on port `55432` inside the `ubb-events-postgres` container.
- Spring Boot on port `8080`.
- Vite on port `5173`.
- Mailpit on port `8025`.

It also enables the `db/demo` migrations only for local development and is the supported way to reset the demonstration state.

## Demo accounts

All demo accounts use:

```text
DemoPass123!
```

- `organizer@example.invalid`: Organizer in AI Society, Member of Robotics Club.
- `ai.admin@example.invalid`: Org Admin of AI Society.
- `robotics.admin@example.invalid`: Org Admin of Robotics Club.
- `applicant@example.invalid`: Student with a pending request to AI Society.
- `member@example.invalid`: Member of AI Society, Organizer in Language Exchange.

## Required frontend role work

### 1. Preserve the mid-term demo end-to-end

Before each rehearsal, run the reset script and execute the documented demo flow:

1. Verify a newly registered account through Mailpit.
2. Sign in as each demo role.
3. Exercise organization discovery and pending join requests.
4. Change a member role and use confirmation dialogs.
5. Create a draft event.
6. Add two activities and an agenda.
7. Publish the event and resolve all readiness violations if shown.
8. Verify the event appears in discovery.
9. Edit, cancel, report and delete or lifecycle-test the event as appropriate.

Primary documentation:

- `docs/demo/mid-term-demo.md`

### 2. Complete the assigned participant frontend workflows

The following screens remain outside the current mid-term demo backend:

- Event detail signup controls.
- Confirmed signups and waitlists.
- Waitlist positions and promotion states.
- `/my-activities` against a real registrations endpoint.
- Promotion notifications.
- Personal QR passes.
- QR and manual attendance.
- Attendance correction boundaries.

These areas must not be presented as complete until matching backend routes, persistence and authorization exist. The frontend currently hides the registration and notification navigation until that backend is available.

The frontend role should therefore:

- Reconcile the participant API contract with the backend before wiring it.
- Replace placeholder route assumptions with real typed endpoints.
- Enforce loading, empty, error and busy states consistently.
- Mirror server-side rules for registration windows, capacity, overlap, waitlists and cancellation cut-off only as UX hints, never as security boundaries.

### 3. Improve visual and interaction polish

Main follow-up tasks:

- Replace remaining ad hoc CSS and inconsistent status treatments with semantic pastel tokens.
- Review every data-heavy page at desktop and mobile sizes.
- Add consistent empty and loading states to organization and authoring lists.
- Test confirmation dialogs with keyboard navigation and focus restoration.
- Verify links, headings, form labels and toast announcements with an accessibility pass.
- Remove stale participant routes that are not part of the current backend demo.
- Ensure no hardcoded demo IDs or mock data remain in production-style UI code.

### 4. Strengthen API integration and session identity

The frontend should continue to use the shared API client and HTTP session, but must remove temporary identity assumptions once the backend is fully reconciled.

The frontend role should verify:

- Every mutation sends the required CSRF information.
- `401` always clears stale UI session state.
- Logout invalidates the server session and redirects consistently.
- Verification and login error messages come from the backend contract.
- Email delivery errors are clear and actionable.
- All requests use `credentials: include` where needed.
- API response types match the backend DTOs exactly.

### 5. Add browser-level verification

Automated Vitest coverage exists for many components, but a complete browser smoke test should cover:

- Registration and verification.
- Login and logout.
- Protected-route redirect behavior.
- Organization membership actions.
- Create → add activity → add agenda → publish.
- Discovery visibility after publishing.
- Edit and cancel lifecycle flows.
- Mobile navigation and dialog behavior.

## Local run instructions

The supported command is:

```bash
export JAVA_HOME=/usr/lib/jvm/java-27-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
bash scripts/run-app.sh
```

The repository script requires Docker and Java. The application is currently running on this machine with:

- Frontend: <http://localhost:5173>
- Backend: <http://localhost:8080>
- Mailpit: <http://localhost:8025>
- API documentation: <http://localhost:8080/v3/api-docs>

To reset before a demonstration:

```bash
bash scripts/run-app.sh --reset
export JAVA_HOME=/usr/lib/jvm/java-27-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
bash scripts/run-app.sh
```

To stop backend and frontend, press `Ctrl+C`. To stop or remove local infrastructure:

```bash
bash scripts/run-app.sh --stop
```

## Current verification

Verified locally on `main`:

- PostgreSQL container started successfully on port `55432`.
- Mailpit started successfully on port `8025`.
- Spring Boot started successfully on port `8080`.
- Vite started successfully on port `5173`.
- Demo login for `organizer@example.invalid` returned `200`.
- Discovery returned the seeded `AI Society Code Sprint` event.
- Mailpit endpoint returned `200`.
- Frontend route `/events` returned `200`.

The backend required a Lombok compatibility repair while building under the available JDK 27 environment. The repaired classes now use explicit constructors and accessors so the backend can compile without Lombok annotation processing.

## Immediate frontend priorities

1. Run the complete mid-term demo script and identify UI/wording defects.
2. Fix any route, redirect or session-edge behavior found during browser testing.
3. Verify organization and authoring pages on narrow and wide viewports.
4. Improve confirmation-dialog, loading, validation and toast consistency.
5. Add browser smoke tests for the supported mid-term flows.
6. Record participant signup/waitlist/attendance as a separately blocked backend slice rather than as completed frontend functionality.
