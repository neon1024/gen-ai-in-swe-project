# Mid-term demo script

Local walkthrough of the integrated application for the mid-term presentation (S2 section A).

## Start

```bash
scripts/run-app.sh            # PostgreSQL in Docker, Spring Boot on :8080, Vite on :5173
scripts/run-app.sh --reset    # before rehearsing: wipe the local database so the seed state is fresh
```

Open http://localhost:5173. `run-app.sh` loads the local demo accounts (`db/demo`); they never load
outside local development. Every demo account uses the password `DemoPass123!`.

| Email | Role |
|---|---|
| `organizer@example.invalid` | Organizer in AI Society, member of Robotics Club |
| `ai.admin@example.invalid` | Org Admin of AI Society |
| `robotics.admin@example.invalid` | Org Admin of Robotics Club |
| `applicant@example.invalid` | Student with a pending request to AI Society |
| `member@example.invalid` | Member of AI Society, Organizer in Language Exchange |

## Flow

1. **Accounts (WF-01).** Open `/signup`, create an account: the app asks to verify the email.
   Signing in before verification is refused with "Verify your email address before signing in.".
   A wrong password shows "Invalid email or password.".
2. **Membership (US-06–US-10).** Sign in as `ai.admin@example.invalid`, open **Organizations**:
   accept the applicant's request, change a member's role. Sign out.
3. **Authoring (US-14–US-18).** Sign in as `organizer@example.invalid`, open **New event**. Only
   organizations where the user is Organizer or Org Admin are offered. Create the draft, add an
   activity and an agenda item, then **Publish event**.
4. **Discovery (US-24/US-25).** Open **Discover**: the published event is listed and filterable by
   keyword, date, organization and category. The title opens the event page.
5. **Lifecycle (US-20–US-22).** On the event page use **Edit event**, **Report** or cancel an
   activity. Cancelling the last scheduled activity cancels the event, which becomes read-only.

## Not in this build

- Participant registration (event details, sign up, waitlist, My activities, notifications, QR,
  attendance; US-26–US-31) has no backend yet, so those screens are hidden from the navigation.
- Organization registration and approval (US-03–US-05) are not implemented; the demo uses the
  seeded organizations.
- Verification emails are sent only when `RESEND_API_KEY` is set. Without it, a new account stays
  pending.
