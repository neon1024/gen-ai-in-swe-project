# GenAI entry template

Copy to a new file for your own session in `docs/genai-usage/entries/`, following the guide's UTC timestamp + issue + human handle + UUID4 convention. Complete every field; do not copy another log's configuration. Entries are repository artifacts and must be in English. Do not store secrets or prompts containing unnecessary personal data. Do not create empty entries merely to satisfy the format.

The first JSON block is stdlib-readable metadata; additional text after it may explain results and limitations. For a task without an issue (such as documentation preparation), use `issues: []`, filename `issue-none`, and explain why. `human_developer: "Unassigned"` means the student responsible for the session was not identified; it does not mean the GitHub issues are unassigned.

```json
{
  "entry_id": "New UUID4 for the session",
  "timestamp": "ISO 8601 timestamp with the actual offset",
  "timezone": "Europe/Bucharest",
  "human_developer": "Unassigned",
  "ai_environment": "Tool/environment actually used",
  "model": "Not confirmed for this task",
  "reasoning_effort": "Not confirmed for this task",
  "prompt_intent": "Actual task objective",
  "issues": [],
  "requirements": [],
  "branch": "Not recorded",
  "pull_requests": [],
  "files": [],
  "verification": [{"command": "Command actually executed", "result": "Observed result", "kind": "AI-assisted verification"}],
  "human_interventions": [],
  "human_review": {"status": "Pending student review", "reviewer": null, "confirmation": null},
  "outcome": "Actual result and limitations"
}
```

Model/effort reported for Developer A/B in the guide do not authorize filling these fields without task-specific confirmation. If human review is confirmed, record the person, date and supporting reference/statement; do not interpret an automated test or AI review as that confirmation. Preserve previous entries and identify later corrections explicitly, without retroactively inventing human actions.
