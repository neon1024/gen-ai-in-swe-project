"""Deterministic GenAI index; run from repository root. --check never writes."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import sys
from uuid import UUID

REQUIRED = {"entry_id", "timestamp", "timezone", "human_developer", "ai_environment",
            "model", "reasoning_effort", "prompt_intent", "issues", "requirements",
            "branch", "pull_requests", "files", "verification", "human_interventions",
            "human_review", "outcome"}

def read_entry(path):
    match = re.search(r"```json\n(.*?)\n```", path.read_text(), re.S)
    if not match:
        raise ValueError(f"Missing JSON metadata: {path.name}")
    data = json.loads(match[1])
    if not REQUIRED <= data.keys():
        raise ValueError(f"Missing fields in {path.name}: {sorted(REQUIRED - data.keys())}")
    uid = UUID(data["entry_id"])
    if uid.version != 4 or str(uid) not in path.name:
        raise ValueError(f"Invalid UUID4 filename: {path.name}")
    stamp = datetime.fromisoformat(data["timestamp"])
    if stamp.utcoffset() is None:
        raise ValueError(f"Timestamp requires timezone offset: {path.name}")
    from zoneinfo import ZoneInfo
    zone = ZoneInfo(data["timezone"])
    if stamp.astimezone(zone).utcoffset() != stamp.utcoffset():
        raise ValueError(f"Timestamp offset disagrees with IANA timezone: {path.name}")
    if not path.name.startswith(stamp.astimezone(timezone.utc).strftime("%Y%m%dT%H%M%SZ") + "-issue-"):
        raise ValueError(f"Filename timestamp mismatch: {path.name}")
    for field in ("issues", "requirements", "pull_requests", "files", "verification", "human_interventions"):
        if not isinstance(data[field], list):
            raise ValueError(f"{field} must be a list: {path.name}")
    review = data["human_review"]
    if review.get("status") != "Pending student review" and not (review.get("reviewer") and review.get("confirmation")):
        raise ValueError(f"Human review requires explicit reviewer and confirmation: {path.name}")
    for check in data["verification"]:
        if not {"command", "result", "kind"} <= check.keys():
            raise ValueError(f"Verification missing command/result/kind: {path.name}")
    return path.name, data

def cell(value):
    return str(value).replace("|", "\\|").replace("\n", " ")

def render_index(entries):
    entries = sorted(entries, key=lambda item: (datetime.fromisoformat(item[1]["timestamp"]), item[1]["entry_id"]))
    ids = [d["entry_id"] for _, d in entries]
    if len(ids) != len(set(ids)):
        raise ValueError("Duplicate entry UUID")
    lines = ["# Primary GenAI usage log", "",
        "Consolidated reporting document. Independent entries provide detailed evidence; model and effort are task-specific confirmations, never inferred from previous sessions. Ownership belongs to the student regardless of the agent. All saved records and reports must be in English.", "",
        "## Policy and updates", "",
        "Follow the [collaboration guide](planning/collaborative-development.md) and [template](genai-usage/entry-template.md). Each agent creates only its own UUID entry. One integrator collects entries and runs `rtk proxy python3 docs/genai-usage/consolidate.py`; `--check` validates without writing. The index is generated, not appended to concurrently. Review added/removed entries in the diff; regenerate after conflict resolution while preserving records.", "",
        "AI-assisted verification (tests/analysis/AI review) is not student review. Human review starts at `Pending student review` and requires identifiable confirmation to change.", "",
        "## Preserved earlier records", "",
        "The [Phase 0 audit](planning/phase-0-audit.md) contains the historical session record. Its prose was translated in Phase 0.1.1 without reinterpreting evidence as confirmation of the current session's model/effort or human review. Historical Project material remains intact and is not replaced by this index.", "",
        "## Per-session entries", "",
        "| Entry / timestamp and timezone | Human developer | Environment / model / effort | Issues | Requirements | Branch / PR | Assisted verification / human review | Outcome |",
        "|---|---|---|---|---|---|---|---|"]
    for name, data in entries:
        issue_refs = ", ".join(f"[#{x['number']}]({x['url']})" for x in data["issues"]) or "No issue linked (documentation-only session)"
        results = "; ".join(str(x["result"]) for x in data["verification"]) or "Not recorded"
        values = [f"[{data['entry_id']}](genai-usage/entries/{name}) — {data['timestamp']} ({data['timezone']})",
            data["human_developer"], f"{data['ai_environment']} / {data['model']} / {data['reasoning_effort']}",
            issue_refs, ", ".join(data["requirements"]) or "Not applicable", f"{data['branch']} / {', '.join(data['pull_requests']) or 'Not recorded'}",
            f"{results}; human review: {data['human_review']['status']}", data["outcome"]]
        lines.append("| " + " | ".join(cell(x) for x in values) + " |")
    lines += ["", "## Reporting and final reflection", "",
        "Intent, files, commands/results and human interventions are fully recorded in each linked entry. Mid-term/final tables should use these records and historical evidence, including actual human decisions, effectiveness and limitations. Student reflection and defense remain pending; an up-to-date index does not imply acceptance.", ""]
    return "\n".join(lines)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path.cwd()
    if not (root / ".git").exists() or not (root / "AGENTS.md").exists():
        parser.error("Run from official Git root")
    folder = root / "docs/genai-usage/entries"
    entries = [read_entry(p) for p in sorted(folder.glob("*.md"))]
    expected = render_index(entries)
    target = root / "docs/genai_usage_log.md"
    if args.check:
        if not target.exists() or target.read_text() != expected:
            print("FAIL: consolidated GenAI log is stale; an integrator must regenerate it", file=sys.stderr)
            return 1
        print(f"PASS: consolidated GenAI log matches {len(entries)} session entries")
    else:
        target.write_text(expected)
        print(f"Consolidated {len(entries)} entries; source entries unchanged")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
