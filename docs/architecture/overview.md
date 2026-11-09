# Architecture baseline

Status: **Accepted technical direction — requesting-student authorization, 2026-11-10**. Independent Duarte review remains Pending student review; no claim of joint approval is made. See the [ADR register](../decisions/README.md). Confirmed in code: Java 21/Spring Boot 4.1.1 backend; no implemented persistence, frontend or security. Course obligations: separate presentation, business logic, persistence and interfaces; persistent data and documented APIs (S1 R7–R9). These requirements do not mandate microservices, an SPA or a database technology.

## Decision summary

Use a feature-oriented modular monolith in the existing backend and a separate React/TypeScript SPA in `frontend/`, REST/JSON, Vite, React Router and Tailwind CSS. Select PostgreSQL, JPA/Hibernate, Flyway, Spring Security sessions/CSRF, Spring Mail/SMTP and local Mailpit. Use Boot-managed JUnit Jupiter (the user confirmed retaining managed version 6.0.3 rather than forcing JUnit 5), Mockito, Spring Boot Test, Testcontainers where appropriate, Vitest/React Testing Library and Playwright. Local infrastructure uses Docker Compose; CI uses GitHub Actions. No new technology is installed by this baseline.

## Alternatives reviewed

| Approach | Advantages | Limitations | Recommendation |
|---|---|---|---|
| Layered monolithic backend organized by capabilities, with a UI and documented APIs | Reuses Spring; local transactions; lower operational effort; straightforward rule testing | Requires internal boundaries and contract coordination | Selected modular-monolith direction; separate React SPA authorized |
| Spring monolith with server-rendered UI | Fewer tools, same origin and simpler execution | Template library absent; QR/UI interaction needs design | Reviewed alternative; not selected |
| Independent domain services | Isolated deployment and evolution | Distributed consistency, authentication and observability increase effort | No demonstrated need; not recommended at this stage |

The requesting student selected the separate SPA and PostgreSQL/JPA/Flyway in ADR-002/003. Preserve configured Java 21, Spring Boot 4.1.1 and Maven versions where compatible; lock and verify remaining versions in M1. Same-origin production UI/API deployment is preferred where practical despite separate source directories. Concrete deployment topology, contract schemas and component library remain follow-up choices, not invented approvals.

## Logical capabilities and responsibilities

| Logical component | Responsibility / data | Consumed/produced interfaces | Dependencies |
|---|---|---|---|
| Identity and access | Account, confirmation, recovery; authorization decisions | Authenticated identity and organization/event policy; email delivery | Email adapter; persistence |
| Organizations | Requests, approval, members, roles and admin invariants | Organization and membership context for events/privacy | Identity |
| Events and discovery | Event → Activity → AgendaItem; states; search and filters | Visible-activity queries; authorized publication and changes | Organizations; clock |
| Registrations | Signup per activity, capacity, FIFO, overlap, cancellation | Registration commands also used by invitations; capacity queries | Visible/active activity; identity; transaction |
| Communication and invitations | Inbox, invitation, email, announcements and reminders | Acceptance invokes the same registration/membership rules; email adapter | Identity, organizations, events and registrations |
| Team and tasks | Owner/Editor/Staff, ownership transfer and Task | Team policy, task assignment/completion | Membership; events |
| Attendance and feedback | Personal QR, check-in/correction and 1–5 rating | Confirmed ↔ Attended; authorized, anonymized feedback | Registrations; team; clock |
| Reports | Per-activity aggregates and CSV without personal data | Metric queries with an approved formula | Registrations, attendance, feedback |

These are logical capabilities within the accepted modular-monolith direction, not created modules or an approved detailed package/API design. Concrete contracts (endpoints, DTOs, errors, schema, migrations) will be defined for each approved slice. The relationships refine the current baseline and do not reuse the historical Event-only model from `Project/`.

## Flow and integrity

UI → API/controller (format validation and authentication) → use case (context authorization and BRs) → persistence (invariants) → domain result translated into the HTTP contract. Controllers should not implement FIFO or ad hoc permissions. Recommendation: simple business interfaces and consistent error contracts; store only necessary data and do not expose personal email addresses.

Registration, cancellation, capacity increase and promotion require an atomic unit to avoid exceeding capacity. Overlap checks also require participant-level protection: locking only the activity does not prevent simultaneous signups for different activities. Select and test the transaction strategy in M4. Publication validates every activity. Cancellation preserves the data needed for reporting instead of deleting historical signups.

After commit, deliver notifications under an explicit failure/retry policy; do not undo a signup because email delivery failed. Start with the smallest reliable solution; use an external queue/outbox only if delivery requirements justify it. Use a controllable clock for automatic completion, registration windows, the 24-hour reminder and check-in/feedback limits. Consider restarts and repeated jobs in the design. University integration, payments and venue booking have no approved requirement.

## Tests and risks

- Unit: BR-01–BR-25, role/context, FIFO and eligibility, time boundaries, hidden private activities, anonymity and formulas.
- Integration: persistence after restart, rollback, duplicates and last-place/participant concurrency; cross-organization denial and APIs without data leaks.
- Contracts: documented responses/errors; invitations reuse the same rules; simulated email in tests and adapter verification in an authorized environment.
- UI/end-to-end: approved organization → published event → discovery → registration; then team/invitation → check-in → feedback → CSV. Validate with real users and record improvements.
- Operations: execution in another environment, reproducible configuration and deployment evidence. No invented coverage percentage, latency or volume.

Main risks: concurrent signups; state/count mismatches; security between contexts; email/job failures and repeats; personal data; late changes after interviews; broad scope and coordination of parallel team contributions. Mitigate through small slices, existing criteria, explicit decisions and boundary tests before extending functionality.

## Actual state and commands

`BackendApplication` starts Spring, `TestController` only implements `GET /` → `Hello`, and `BackendApplicationTests.contextLoads` is the only test. No product rule was found in code. See actual commands and limits in [AGENTS](../../AGENTS.md). Detailed plans will use `writing-plans` only after review of these ADRs and approval of the specific slice design and pending decisions. The technical baseline itself is now authorized.

## Shared contract coordination

All agents use the same accepted ADRs and contracts. Before changing an API, schema, auth policy or shared configuration, document the proposal, affected issues, compatibility and tests; obtain human approval and update the common contract. One owner executes a shared change at a time; other contributors consume the agreed version. See the [collaboration guide](../planning/collaborative-development.md). All architecture artifacts and decisions must be in English.

## Requirement reconciliation and implementation gates

No selected technology contradicts S1 R7–R12: the course mandates layered architecture, persistent data, documented APIs, usable UI, automated testing and reproducible deployment, without naming an incompatible framework. REST/SPA sessions still enforce all approved roles and privacy rules. See [baseline and kickoff](../planning/baseline-and-kickoff.md) for actual issue dependencies.

The JUnit 5 versus Boot-managed Jupiter 6.0.3 mismatch was explicitly resolved by the requesting student in favor of the managed version (ADR-006). Runtime independence from LLMs does not replace the separate no-GenAI authorship requirement (ADR-008). Q-04–11 remain only as scoped below in the decision register. These questions must be answered before dependent implementation; they do not invalidate this documentation baseline.
