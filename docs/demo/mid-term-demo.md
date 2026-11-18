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
  Exchange, Photography Group), their memberships, one pending join request from
  `applicant@example.invalid` to AI Society, and one published event: **AI Society Code Sprint**,
  owned by `organizer@example.invalid`. Its "Code competition" activity takes place seven days after
  the database was created, 14:00–18:00, with the agenda Challenge revealed → Teams implement their
  solutions → Projects presented → Results announced.
- Run `scripts/run-app.sh --reset` before each rehearsal to restore the seed state and remove
  accounts and events created earlier.

Important actions (role changes, rejecting a request, removing a member, leaving an organization,
publishing, removing an activity, cancelling and deleting) ask for confirmation in a dialog.

## Flow

1. **Accounts (WF-01).**
   - Open `/signup` and register a new email. The app asks you to verify the email.
   - Sign in with that account: refused with "Verify your email address before signing in.".
   - Verify the email (see [Verifying the new account](#verifying-the-new-account)), then sign in
     again: it succeeds.
   - Sign in as `organizer@example.invalid` with a wrong password: "Invalid email or password.".
2. **Membership (US-06–US-10).** Sign in as `ai.admin@example.invalid`, open **Organizations**.
   - **My organizations** lists AI Society; **Discover** lists the organizations to join.
   - Under AI Society, **Pending join requests**: click **Accept** on Demo Applicant's request.
   - Under **Members**, change a member's role in its dropdown (for example Demo Member → Organizer)
     and confirm **Change role**.
   - Sign out.
3. **Authoring (US-14–US-17).** Sign in as `organizer@example.invalid`.
   - Open **My events**: the seeded Code Sprint is listed under Published; open it to show the
     activity and its agenda.
   - Open **New event**. The **Organization** list offers only AI Society, where this user is
     Organizer (they are a plain member of Robotics Club).
   - Fill in **Title**, **Category** and **Description**, then click **Create draft**. The draft
     now appears under **My events → Drafts**.
   - **Add activity** twice, so that cancelling one in step 5 leaves the event running. For each:
     title, description, a **Start** date and time (the end and registration window are suggested
     from it), a **Location** or **Online link**, and **Capacity**.
   - On one activity, add an agenda item: **Agenda time** and **Agenda title**, optionally
     **+ Add speaker**.
   - Click **Publish event** and confirm. If something is missing, the page lists every problem at
     once.
4. **Discovery (US-24/US-25).** Open **Discover**: upcoming events are listed without searching,
   the Code Sprint and the new event among them. Filter by keyword, date, organization (AI Society)
   and category, then click the new event's title to open its page.
5. **Lifecycle (US-18, US-20–US-22).** On the event page:
   - **Edit event**: change the title, click **Save details**, go back.
   - **Report** shows the event report.
   - **Cancel activity** on one activity and confirm: the event stays published.
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
