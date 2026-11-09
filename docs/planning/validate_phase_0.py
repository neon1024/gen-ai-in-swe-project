"""Read-only checks for the Phase 0 documentation; run from the Git root."""
from pathlib import Path
from urllib.parse import unquote, urlsplit
import collections
import hashlib
import json
import re

root = Path.cwd()
assert (root / ".git").exists(), "Run from the official Git root"
assert not (root / "gen-ai-in-swe-project").exists(), "Nested root found"
req = (root / "docs/requirements/02-requirements.md").read_text()
trace = (root / "docs/requirements/traceability.md").read_text()
original_trace = (root / "docs/requirements/07-traceability.md").read_text()
backlog = (root / "docs/planning/backlog.md").read_text()
stories_text = (root / "docs/requirements/08-user-stories.md").read_text()
overview = (root / "docs/requirements/overview.md").read_text()
snapshot = json.loads((root / "docs/planning/github-issues-snapshot.json").read_text())

def row_ids(text, pattern):
    return re.findall(r"^\| (" + pattern + r") ", text, re.M)

fr = row_ids(req, r"FR-[A-Z]+-\d+")
br = row_ids(req, r"BR-\d+")
assert len(fr) == len(set(fr)) == 51
assert len(br) == len(set(br)) == 25
assert set(fr) == set(row_ids(trace, r"FR-[A-Z]+-\d+"))
assert set(br) == set(row_ids(trace, r"BR-\d+"))
assert set(fr) == set(row_ids(original_trace, r"FR-[A-Z]+-\d+"))
assert set(br) == set(row_ids(original_trace, r"BR-\d+"))
assert len(re.findall(r"^### UC-\d+ ", (root / "docs/requirements/04-use-cases.md").read_text(), re.M)) == 31
assert len(re.findall(r"^## WF-\d+ ", (root / "docs/requirements/05-workflows.md").read_text(), re.M)) == 6
print("PASS: 51 FR, 25 BR, 31 UC, 6 WF; original and development matrices cover every FR/BR")

stories = {}
for section in re.split(r"^### ", stories_text, flags=re.M)[1:]:
    sid = re.match(r"US-\d+", section)[0]
    assert sid not in stories
    stories[sid] = section
assert set(stories) == {f"US-{n:02}" for n in range(1, 51)}
blocks = {}
for section in re.split(r"^### ", backlog, flags=re.M)[1:]:
    tid = re.search(r"T-US-\d+", section.splitlines()[0])[0]
    assert tid not in blocks
    blocks[tid] = section
assert set(blocks) == {"T-" + sid for sid in stories}
edges = {}
for tid, section in blocks.items():
    dep_line = re.search(r"^- Dependencies: (.*)$", section, re.M)[1]
    edges[tid] = re.findall(r"T-US-\d+", dep_line)
    for dep in edges[tid]:
        assert dep in blocks, (tid, dep)
        a = int(re.search(r"implementation / M(\d+)", blocks[dep])[1])
        b = int(re.search(r"implementation / M(\d+)", section)[1])
        assert a <= b, (tid, "depends on later milestone", dep)
    for label in ("Objective:", "Expected components:", "Acceptance:", "Tests:", "Initial status:"):
        assert label in section, (tid, label)
    sid = tid[2:]
    refs = re.search(r"\*\*Traces:\*\* (.*)", stories[sid])[1]
    assert refs in section, (tid, "story references changed")
    assert tid in trace, (tid, "missing in traceability")
active, done = set(), set()
def visit(tid):
    assert tid not in active, ("Dependency cycle", tid)
    if tid in done:
        return
    active.add(tid)
    for dep in edges[tid]:
        visit(dep)
    active.remove(tid)
    done.add(tid)
for tid in blocks:
    visit(tid)
assert set(re.findall(r"^\| (P-\d+) ", backlog, re.M)) == {f"P-{i:02}" for i in range(1, 10)}
print("PASS: 50 stories mapped once to tasks; 9 support tasks; story dependency graph acyclic and milestone order valid")

for prefix, count in (("NFR", 7), ("TC", 3), ("DEL", 9)):
    ids = set(row_ids(overview, prefix + r"-\d+"))
    assert ids == {f"{prefix}-{i:03}" for i in range(1, count + 1)}
    assert ids == set(row_ids(trace, prefix + r"-\d+"))
assert all("| R" + str(i) + " " in trace or (i in (13, 14) and "| R13–R14 " in trace) for i in range(1, 17))
print("PASS: 7 NFR, 3 TC, 9 DEL traced to tasks; R1–R16 source coverage present")

issues = snapshot["issues"]
assert len(issues) == len({x["number"] for x in issues}) == 56
remote_stories = [x for x in issues if x["title"].startswith("US-")]
epics = [x for x in issues if x["title"].startswith("WF-")]
assert len(remote_stories) == 50 and len(epics) == 6
assert all(x["state"] == "open" and not x["assignees"] and not x["comments"] for x in issues)
for issue in remote_stories:
    sid = re.match(r"US-\d+", issue["title"])[0]
    local = stories[sid]
    assert re.findall(r"^- \[ \] (.*)", local, re.M) == re.findall(r"^- \[ \] (.*)", issue["body"], re.M), sid
    milestone = re.search(r"\*\*Milestone:\*\* (.*?) ·", local)[1]
    assert issue["milestone"] == milestone, sid
    parent = next(x for x in epics if issue["parent"].endswith("/" + str(x["number"])))
    wf = re.match(r"WF-\d+", parent["title"])[0]
    assert wf in issue["labels"] and wf in issue["body"], sid
for epic in epics:
    children = [x for x in remote_stories if x["parent"].endswith("/" + str(epic["number"]))]
    assert len(children) == epic["sub_issues"]["total"]
assert collections.Counter(x["milestone"] for x in issues) == {"Mid-term": 29, "Final": 27}
assert len(snapshot["milestones"]) == 2
assert all(x["due_on"] is None for x in snapshot["milestones"])
print("PASS: snapshot of 56 issues/2 milestones; 50/50 acceptance criteria and milestones match; parent relations consistent")

manifest = (root / "docs/sources/README.md").read_text()
source_count = 0
external_sources_compared = 0
for copy in (root / "docs/sources/course").iterdir():
    assert copy.is_file()
    original = root.parent / "Project" / copy.name
    digest = hashlib.sha256(copy.read_bytes()).hexdigest()
    # A fresh clone is self-contained: archived sources and their manifest are authoritative.
    # Compare optional original workspace material only when it is available.
    if original.is_file():
        assert copy.read_bytes() == original.read_bytes(), copy.name
        external_sources_compared += 1
    assert digest in manifest, copy.name
    source_count += 1
assert source_count == 6
print(f"PASS: 6 archived source hashes match the manifest; {external_sources_compared} optional workspace originals compared")

link_count = 0
for file in [root / "AGENTS.md", root / "CLAUDE.md", root / "README.md", *sorted((root / "docs").rglob("*.md"))]:
    text = file.read_text()
    assert text.count("```") % 2 == 0, (file, "unclosed fence")
    for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", text):
        if target.startswith(("http://", "https://", "mailto:")):
            continue
        path = unquote(urlsplit(target).path)
        if path:
            assert (file.parent / path).exists(), (file, target)
            link_count += 1
print(f"PASS: {link_count} local Markdown links resolve; code fences balanced")
assert len(re.findall(r"^### Prompt \d+", (root / "docs/prompts/00-development-guide.md").read_text(), re.M)) == 8
print("PASS: 8 sequential future prompts; no application tests executed by this validator")

# Phase 0.1: current coordination fields, without altering the Phase 0 snapshot.
coordination = json.loads((root / "docs/planning/github-coordination-snapshot.json").read_text())
current = {re.match(r"US-\d+", x["title"])[0]: x for x in coordination["issues"] if x["title"].startswith("US-")}
assert len(current) == 50
for tid, section in blocks.items():
    issue = current[tid[2:]]
    assert f"[#{issue['number']}]({issue['url']})" in section
    owners = ", ".join(issue["assignees"]) or "Unassigned"
    assert f"Human owner (GitHub Phase 0.1): {owners};" in section
    for label in ("Development status:", "Branch:", "PR:", "Verification:", "Human review: Pending student review", "Shared scope:"):
        assert label in section, (tid, label)
assert {"jose03pedro", "DuarteLagoela"} <= {name for issue in current.values() for name in issue["assignees"]}
guide = (root / "docs/planning/collaborative-development.md").read_text()
assert all(token in guide for token in ("Sol 6.1", "Opus 5.5", "Medium", "jose03pedro", "DuarteLagoela", "Pending student review", "Unassigned"))
assert not list(root.glob("*/AGENTS.md")), "Unexpected model-specific instruction files"
prompts = (root / "docs/prompts/00-development-guide.md").read_text()
for section in re.split(r"^### Prompt ", prompts, flags=re.M)[1:]:
    for field in ("> Issue:", "> Human developer:", "> Branch/worktree:", "> Allowed components:", "> Dependencies:", "> ADRs/contracts:", "> Skills:", "> Requirements:", "> Tests/checks:", "> Logging/verification:", "> Review/integration:"):
        assert field in section, (section.splitlines()[0], field)
print("PASS: Phase 0.1 owners match current snapshot; all 50 issues have coordination fields; 8 prompts have the shared contract")

# The same language policy governs every coding agent.
agent_instructions = (root / "AGENTS.md").read_text()
claude_instructions = (root / "CLAUDE.md").read_text()
assert "## Repository Language Policy" in agent_instructions
assert "English is mandatory" in agent_instructions
assert "[AGENTS.md](AGENTS.md)" in claude_instructions
assert "English" in claude_instructions and "PT-PT" in claude_instructions
assert "## Repository language policy" in prompts
print("PASS: shared English-only repository policy; minimal CLAUDE entry point delegates to AGENTS")

# Accepted technical direction does not assert peer approval or installed functionality.
adr_files = sorted((root / "docs/decisions").glob("ADR-*.md"))
assert len(adr_files) == 8
for number, file in enumerate(adr_files, 1):
    body = file.read_text()
    assert f"ADR-{number:03}" in body
    assert "Accepted for the development baseline" in body
    assert "requesting-student authorization" in body
    assert "Pending student review" in body
    for section in ("## Context", "## Alternatives considered", "## Decision", "## Consequences and risks", "## Requirements and issue impact", "## Follow-up and validation"):
        assert section in body, (file, section)
assert "Boot-managed JUnit Jupiter" in (root / "docs/decisions/ADR-006-testing-strategy.md").read_text()
kickoff = (root / "docs/planning/baseline-and-kickoff.md").read_text()
assert "jose03pedro / Sol 6.1 Medium" in kickoff
assert "DuarteLagoela / Opus 5.5 Medium" in kickoff
assert "issues/17" in kickoff and "issues/25" in kickoff
assert "Not currently unblocked" in kickoff
assert "No application code" in kickoff
print("PASS: 8 accepted requesting-student ADRs with peer review pending; managed JUnit and honest issue kickoff gates documented")
