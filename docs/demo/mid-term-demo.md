# Mid-term demo script

Local walkthrough of the integrated application for the mid-term presentation (S2 section A).

## Start

```bash
scripts/run-app.sh            # PostgreSQL and Mailpit in Docker, Spring Boot on :8080, Vite on :5173
scripts/run-app.sh --reset    # before rehearsing: wipe the local database and inbox so the seed state is fresh
```

Open http://localhost:5173 for the app and http://localhost:8025 in a second tab for the inbox
(Mailpit), where every email the app sends arrives. `run-app.sh` loads the local demo accounts
(`db/demo`); they never load outside local development.

## Demo accounts

Every demo account uses the same password:

```text
DemoPass123!
```

Organizer in AI Society, member of Robotics Club:

```text
organizer@example.invalid
```

Org Admin of AI Society:

```text
ai.admin@example.invalid
```

Org Admin of Robotics Club:

```text
robotics.admin@example.invalid
```

Student with a pending request to AI Society:

```text
applicant@example.invalid
```

Member of AI Society, Organizer in Language Exchange:

```text
member@example.invalid
```

## Seed data

- **Seeded:** the five accounts above, four organizations (AI Society, Robotics Club, Language
  Exchange, Photography Group), their memberships, and one pending join request from
  `applicant@example.invalid` to AI Society.
- **Not seeded:** events. Discover is empty until step 3 publishes one, so run the flow in order.
  Run `scripts/run-app.sh --reset` before each rehearsal to restore the pending request and remove
  events created earlier.

## Flow

1. **Accounts (WF-01).**
   - Open `/signup` and register a new email. The app asks you to verify the email.
   - Sign in with that account: refused with "Verify your email address before signing in.".
   - Verify the email (see [Verifying the new account](#verifying-the-new-account)), then sign in
     again: it succeeds.
   - Sign in as `organizer@example.invalid` with a wrong password: "Invalid email or password.".
2. **Membership (US-06–US-10).** Sign in as `ai.admin@example.invalid`, open **Organizations**.
   - Under AI Society, **Pending join requests**: click **Accept** on the applicant's request.
   - Under **Members**, change a member's role in its dropdown (for example Member → Organizer).
     Members are listed by user ID.
   - Sign out.
3. **Authoring (US-14–US-17).** Sign in as `organizer@example.invalid`, open **New event**.
   - The **Organization** list offers only AI Society, where this user is Organizer (they are a
     plain member of Robotics Club).
   - Fill in **Title**, **Description** and **Category**, then click **Create draft**.
   - **Add activity** twice, so that cancelling one in step 5 leaves the event running. For each:
     title, description, **Start**/**End** a few days ahead, a **Location** or **Online link**,
     **Capacity**, and **Registration opens**/**Registration closes** ending before **Start**.
   - On one activity, **Add agenda item** with an **Agenda time** inside the activity.
   - Click **Publish event**. If something is missing, the page lists every problem at once.
4. **Discovery (US-24/US-25).** Open **Discover**: the event is listed. Filter by keyword, date,
   organization (AI Society) and category, then click the title to open the event page.
5. **Lifecycle (US-18, US-20–US-22).** On the event page:
   - **Edit event**: change the title, click **Save details**, go back.
   - **Report** shows the event report.
   - **Cancel activity** on one activity: the event stays published.
   - **Cancel activity** on the other one: the event is cancelled and shows "This event is
     read-only.".

## Verifying the new account

`run-app.sh` starts Mailpit, a local inbox that catches every email the app sends, for any address.
Open http://localhost:8025 in a second tab, open the "Verify your email address" message and click
**Verify email**. The link opens `/verify-email` on http://localhost:5173 and activates the account.
Register as many accounts as needed, with any made-up addresses. `--reset` also empties the inbox.

To send real email instead, start the app with a Resend key (then Mailpit is not used):

```bash
source ~/.config/ubb-events/resend.env && scripts/run-app.sh
```

Resend's shared `onboarding@resend.dev` sender only delivers to the Resend account's own address;
any other recipient makes sign-up fail with "We couldn't send the verification email. Try again
later.". A verified domain in Resend lifts that limit.

## Not in this build

- Participant registration (event details, sign up, waitlist, My activities, notifications, QR,
  attendance; US-26–US-31) has no backend yet, so those screens are hidden from the navigation.
- Organization registration and approval (US-03–US-05) are not implemented; the demo uses the
  seeded organizations.
