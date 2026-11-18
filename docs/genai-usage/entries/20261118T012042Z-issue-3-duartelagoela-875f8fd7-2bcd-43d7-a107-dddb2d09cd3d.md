# Local Mailpit inbox for verification emails and clear email-delivery errors

Mid-term demo preparation: the demo needs several sign-ups with different addresses, but Resend's shared test sender only delivers to the Resend account owner's address. Locally, verification emails now go to Mailpit; a failed send now returns a clear error instead of a misleading 401.

```json
{
  "entry_id": "875f8fd7-2bcd-43d7-a107-dddb2d09cd3d",
  "timestamp": "2026-11-18T03:20:42+02:00",
  "timezone": "Europe/Bucharest",
  "human_developer": "DuarteLagoela",
  "ai_environment": "Claude Code (VS Code extension)",
  "model": "Claude Opus 5.5",
  "reasoning_effort": "Not confirmed for this task",
  "prompt_intent": "Prepare and run the mid-term demo locally: summarize implemented scope, fix the local environment, make the demo script specific, set up verification emails, support several sign-ups with different addresses, and fix the 401 shown when an email cannot be sent.",
  "issues": [3],
  "requirements": ["US-01"],
  "branch": "fix/local-mail-inbox",
  "pull_requests": [],
  "files": [
    "backend/pom.xml",
    "backend/src/main/java/com/ubb/genai/auth/service/EmailService.java",
    "backend/src/main/java/com/ubb/genai/auth/controller/AuthController.java",
    "backend/src/main/java/com/ubb/genai/auth/domain/exception/EmailDeliveryException.java",
    "backend/src/test/java/com/ubb/genai/auth/service/EmailServiceTest.java",
    "backend/src/test/java/com/ubb/genai/auth/controller/AuthControllerEmailFailureTest.java",
    "scripts/run-app.sh",
    "docs/demo/mid-term-demo.md",
    "README.md"
  ],
  "verification": [
    {
      "command": "bash backend/mvnw -q -f backend/pom.xml test -Dtest='EmailServiceTest,AuthControllerEmailFailureTest' (before implementation)",
      "result": "Failed to compile: EmailService had no SMTP-capable constructor (expected red step)",
      "kind": "AI-assisted verification"
    },
    {
      "command": "bash backend/mvnw -q -f backend/pom.xml test -Dtest='EmailServiceTest,AuthControllerEmailFailureTest' (after implementation)",
      "result": "7 tests run, 0 failures, 0 errors",
      "kind": "AI-assisted verification"
    },
    {
      "command": "bash backend/mvnw -q -f backend/pom.xml test",
      "result": "Exit 0; 328 tests run, 0 failures, 0 errors, 0 skipped",
      "kind": "AI-assisted verification"
    },
    {
      "command": "scripts/run-app.sh without RESEND_API_KEY; POST /api/auth/register for alice@example.com and bob@example.com; Mailpit API GET /api/v1/messages; POST /api/auth/verification/confirm with Alice's token; POST /api/auth/login for both",
      "result": "Both registrations 201; Mailpit held one 'Verify your email address' message per address; link pointed to http://localhost:5173/verify-email; confirm 200; Alice login 200; Bob (unverified) 401",
      "kind": "AI-assisted verification"
    },
    {
      "command": "docker stop ubb-events-mailpit; POST /api/auth/register for carol@example.com; SELECT count(*) FROM users WHERE email='carol@example.com'",
      "result": "503 {code: EMAIL_DELIVERY_FAILED, detail: \"We couldn't send the verification email. Try again later.\"}; 0 rows (registration rolled back)",
      "kind": "AI-assisted verification"
    },
    {
      "command": "POST /api/auth/register with a plus-addressed variant of the Resend account email, Resend key set (before the fix)",
      "result": "Resend returned 403 (test sender only delivers to the account's own address); the app answered 401 'Authentication required', which motivated the fix",
      "kind": "AI-assisted verification"
    },
    {
      "command": "Manual test in the browser: sign-up, verification email in Mailpit, Verify email link",
      "result": "Student (DuarteLagoela) reported that email delivery and verification work",
      "kind": "Human verification"
    }
  ],
  "human_interventions": [
    "Chose Resend's shared test sender and an API key stored outside the repository over domain verification, a CLI login or an MCP plugin.",
    "Rejected the single-recipient limitation for the demo: several sign-ups with different addresses must work.",
    "Chose a local Mailpit inbox over verifying a Resend domain or logging the verification link.",
    "Asked to fix the misleading 401 returned when a verification email cannot be sent."
  ],
  "human_review": {"status": "Pending student review", "reviewer": null, "confirmation": null},
  "outcome": "Locally verified: run-app.sh starts Mailpit and the backend sends verification emails through SMTP when no Resend key is set, so any address works and messages appear at http://localhost:8025. A failed send returns 503 EMAIL_DELIVERY_FAILED and rolls back the registration. Not committed; no PR. compose.yaml has no Mailpit service; containerized runs still rely on Resend."
}
```
