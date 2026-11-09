# GitHub issue analysis

**Phase 0 history:** observations about absent assignees below refer to the initial query. For current ownership, consult the [Phase 0.1 snapshot](github-coordination-snapshot.json) and [backlog](backlog.md); historical records were not replaced. The prose was translated in Phase 0.1.1 without changing the findings.

Query dated 2026-11-10 for [neon1024/gen-ai-in-swe-project](https://github.com/neon1024/gen-ai-in-swe-project). Read-only analysis requested by the user; no issue creation, editing, comments, closure or assignment.

## Method and limitations

`git` identified root, history, branch and remote. `gh repo view`, `gh issue list --state all --limit 300 --json …` and `gh api …/milestones?state=all` were attempted from the root: exit code 4, missing authentication (`gh auth login`). To avoid changing credentials/configuration, the query used the public GitHub REST API through Python/CLI: `GET /repos/neon1024/gen-ai-in-swe-project/issues?state=all&per_page=100` and `GET /repos/neon1024/gen-ai-in-swe-project/milestones?state=all&per_page=100`. Neither response had further pagination. Projects boards, additional remote branches and other repositories were not inspected.

The [reduced snapshot](github-issues-snapshot.json) retains title, full body, state, labels, milestone, assignees, parent/sub-issues, dependency summary, comments and update timestamp. It is dated evidence, not live state. The issues endpoint also returned PR #1, excluded from the issue count. The requirements PR is merged and consistent with local HEAD; it does not implement the stories.

## Findings

- 56 issues: six WF epics and 50 stories US-01–50, with no missing/duplicate IDs; all open.
- 50/50 acceptance criteria match local checkboxes in order; no story/document milestone mismatch was found.
- Every story has the expected parent epic and workflow labels; six epics total 50 sub-issues, zero complete.
- No issue had an assignee or comment at that query. Dependency summaries recorded zero blockers/relationships; this does not mean technical independence.
- Mid-term: 29 issues, comprising 26 stories and three epics. Final: 27 issues, comprising 24 stories and three epics. Both open, without `due_on`; counts are not delivered-functionality percentages.

## Organization by workflow

| Epic | Stories | Distribution | Observation |
|---|---|---|---|
| [#2 WF-01 Account and organization onboarding](https://github.com/neon1024/gen-ai-in-swe-project/issues/2) | 13 | 3 Final, 10 Mid-term | Epic is in Mid-term but includes Final stories; complete closure follows Final children |
| [#16 WF-02 Event lifecycle](https://github.com/neon1024/gen-ai-in-swe-project/issues/16) | 10 | 9 Mid-term, 1 Final | Epic is in Mid-term but includes Final stories; complete closure follows Final children |
| [#27 WF-03 Discovery and registration](https://github.com/neon1024/gen-ai-in-swe-project/issues/27) | 8 | 7 Mid-term, 1 Final | Epic is in Mid-term but includes Final stories; complete closure follows Final children |
| [#36 WF-04 Invitations and inbox](https://github.com/neon1024/gen-ai-in-swe-project/issues/36) | 6 | 6 Final | Uniform milestone |
| [#43 WF-05 Event preparation](https://github.com/neon1024/gen-ai-in-swe-project/issues/43) | 6 | 6 Final | Uniform milestone |
| [#50 WF-06 Event day and follow-up](https://github.com/neon1024/gen-ai-in-swe-project/issues/50) | 7 | 7 Final | Uniform milestone |

## Gaps and proposed recommendations

| Priority | Evidence / problem | Proposed local action |
|---|---|---|
| Before implementation | No technical dependencies or owners; stories assume identity, organization and event | Approve P-01/P-03; use T-US backlog predecessors before selecting an issue; remote assignment only if requested |
| Before US-01/02 (#3/#4) | Mid-term confirmation/reset email; US-37 (#42) only Final | Provide a minimal email adapter in M2; general notifications remain M5 |
| Before event permissions | US-09/10 (#11/#12) prevent Owner departure; US-39 (#45) transfers ownership only in Final | Resolve Q-07; enforce refusal at mid-term, without claiming delivered transfer |
| Before registrations | US-27/29 (#31/#33) define capacity, FIFO and overlap, but lack explicit concurrency cases | Transaction design and last-place/same-user cross-activity tests in M4 |
| Before Final | US-35 (#40) closes an invitation under UC-22, but refused registration leaves policy ambiguous | Resolve Q-06 before invitation acceptance |
| Before reports | US-49 (#56) defines a rate without clarifying Attended counting or empty sets | Resolve Q-08 with examples and metric tests |
| Course deliveries | The 56 issues cover product; no standalone issues for research, architecture/API/database, setup/deployment, GenAI or contribution reports | Cover locally through P-01–09 and NFR/TC/DEL; do not create duplicate issues without a request |
| Milestone management | WF-01/02/03 have Final children but Mid-term epics | Assess completion by story/delivery gate; do not close the entire epic at mid-term |
| Completion | Remote checkboxes are unchecked and local product code is absent | Retain not-implemented state; require tests, PR, review and merge under existing DoD |

## Complete inventory

| Issue | Story/epic | State | Milestone | Parent |
|---|---|---|---|---|
| [#2](https://github.com/neon1024/gen-ai-in-swe-project/issues/2) | WF-01 Account and organization onboarding | open | Mid-term | — |
| [#3](https://github.com/neon1024/gen-ai-in-swe-project/issues/3) | US-01 Register an account | open | Mid-term | #2 |
| [#4](https://github.com/neon1024/gen-ai-in-swe-project/issues/4) | US-02 Log in, log out and reset password | open | Mid-term | #2 |
| [#5](https://github.com/neon1024/gen-ai-in-swe-project/issues/5) | US-03 Request organization registration | open | Mid-term | #2 |
| [#6](https://github.com/neon1024/gen-ai-in-swe-project/issues/6) | US-04 Review organization requests | open | Mid-term | #2 |
| [#7](https://github.com/neon1024/gen-ai-in-swe-project/issues/7) | US-05 Edit the organization profile | open | Mid-term | #2 |
| [#8](https://github.com/neon1024/gen-ai-in-swe-project/issues/8) | US-06 Ask to join an organization | open | Mid-term | #2 |
| [#9](https://github.com/neon1024/gen-ai-in-swe-project/issues/9) | US-07 Handle join requests | open | Mid-term | #2 |
| [#10](https://github.com/neon1024/gen-ai-in-swe-project/issues/10) | US-08 Change member roles | open | Mid-term | #2 |
| [#11](https://github.com/neon1024/gen-ai-in-swe-project/issues/11) | US-09 Remove a member | open | Mid-term | #2 |
| [#12](https://github.com/neon1024/gen-ai-in-swe-project/issues/12) | US-10 Leave an organization | open | Mid-term | #2 |
| [#13](https://github.com/neon1024/gen-ai-in-swe-project/issues/13) | US-11 Deactivate user accounts | open | Final | #2 |
| [#14](https://github.com/neon1024/gen-ai-in-swe-project/issues/14) | US-12 Manage Platform Admins | open | Final | #2 |
| [#15](https://github.com/neon1024/gen-ai-in-swe-project/issues/15) | US-13 Deactivate organizations | open | Final | #2 |
| [#16](https://github.com/neon1024/gen-ai-in-swe-project/issues/16) | WF-02 Event lifecycle | open | Mid-term | — |
| [#17](https://github.com/neon1024/gen-ai-in-swe-project/issues/17) | US-14 Create a draft event | open | Mid-term | #16 |
| [#18](https://github.com/neon1024/gen-ai-in-swe-project/issues/18) | US-15 Add and edit activities | open | Mid-term | #16 |
| [#19](https://github.com/neon1024/gen-ai-in-swe-project/issues/19) | US-16 Manage an activity's agenda | open | Mid-term | #16 |
| [#20](https://github.com/neon1024/gen-ai-in-swe-project/issues/20) | US-17 Publish an event | open | Mid-term | #16 |
| [#21](https://github.com/neon1024/gen-ai-in-swe-project/issues/21) | US-18 Edit a published event | open | Mid-term | #16 |
| [#22](https://github.com/neon1024/gen-ai-in-swe-project/issues/22) | US-19 Notify participants of changes and cancellations | open | Final | #16 |
| [#23](https://github.com/neon1024/gen-ai-in-swe-project/issues/23) | US-20 Cancel an activity | open | Mid-term | #16 |
| [#24](https://github.com/neon1024/gen-ai-in-swe-project/issues/24) | US-21 Cancel an event | open | Mid-term | #16 |
| [#25](https://github.com/neon1024/gen-ai-in-swe-project/issues/25) | US-22 Delete a draft event | open | Mid-term | #16 |
| [#26](https://github.com/neon1024/gen-ai-in-swe-project/issues/26) | US-23 Complete events automatically | open | Mid-term | #16 |
| [#27](https://github.com/neon1024/gen-ai-in-swe-project/issues/27) | WF-03 Discovery and registration | open | Mid-term | — |
| [#28](https://github.com/neon1024/gen-ai-in-swe-project/issues/28) | US-24 Browse and search events | open | Mid-term | #27 |
| [#29](https://github.com/neon1024/gen-ai-in-swe-project/issues/29) | US-25 Filter events | open | Mid-term | #27 |
| [#30](https://github.com/neon1024/gen-ai-in-swe-project/issues/30) | US-26 View event details | open | Mid-term | #27 |
| [#31](https://github.com/neon1024/gen-ai-in-swe-project/issues/31) | US-27 Sign up for an activity | open | Mid-term | #27 |
| [#32](https://github.com/neon1024/gen-ai-in-swe-project/issues/32) | US-28 Cancel a signup or leave a waitlist | open | Mid-term | #27 |
| [#33](https://github.com/neon1024/gen-ai-in-swe-project/issues/33) | US-29 Promote from the waitlist automatically | open | Mid-term | #27 |
| [#34](https://github.com/neon1024/gen-ai-in-swe-project/issues/34) | US-30 Notify promoted participants | open | Final | #27 |
| [#35](https://github.com/neon1024/gen-ai-in-swe-project/issues/35) | US-31 View my activities | open | Mid-term | #27 |
| [#36](https://github.com/neon1024/gen-ai-in-swe-project/issues/36) | WF-04 Invitations and inbox | open | Final | — |
| [#37](https://github.com/neon1024/gen-ai-in-swe-project/issues/37) | US-32 Invite a user to an organization | open | Final | #36 |
| [#38](https://github.com/neon1024/gen-ai-in-swe-project/issues/38) | US-33 Make an activity private | open | Final | #36 |
| [#39](https://github.com/neon1024/gen-ai-in-swe-project/issues/39) | US-34 Invite users to a private activity | open | Final | #36 |
| [#40](https://github.com/neon1024/gen-ai-in-swe-project/issues/40) | US-35 Respond to an invitation | open | Final | #36 |
| [#41](https://github.com/neon1024/gen-ai-in-swe-project/issues/41) | US-36 Use the inbox | open | Final | #36 |
| [#42](https://github.com/neon1024/gen-ai-in-swe-project/issues/42) | US-37 Receive notifications by email | open | Final | #36 |
| [#43](https://github.com/neon1024/gen-ai-in-swe-project/issues/43) | WF-05 Event preparation | open | Final | — |
| [#44](https://github.com/neon1024/gen-ai-in-swe-project/issues/44) | US-38 Manage Staff and Editors | open | Final | #43 |
| [#45](https://github.com/neon1024/gen-ai-in-swe-project/issues/45) | US-39 Transfer event ownership | open | Final | #43 |
| [#46](https://github.com/neon1024/gen-ai-in-swe-project/issues/46) | US-40 Create and assign tasks | open | Final | #43 |
| [#47](https://github.com/neon1024/gen-ai-in-swe-project/issues/47) | US-41 Complete tasks and track status | open | Final | #43 |
| [#48](https://github.com/neon1024/gen-ai-in-swe-project/issues/48) | US-42 Send an announcement | open | Final | #43 |
| [#49](https://github.com/neon1024/gen-ai-in-swe-project/issues/49) | US-43 Receive activity reminders | open | Final | #43 |
| [#50](https://github.com/neon1024/gen-ai-in-swe-project/issues/50) | WF-06 Event day and follow-up | open | Final | — |
| [#51](https://github.com/neon1024/gen-ai-in-swe-project/issues/51) | US-44 Get a personal QR code | open | Final | #50 |
| [#52](https://github.com/neon1024/gen-ai-in-swe-project/issues/52) | US-45 Check in by QR code | open | Final | #50 |
| [#53](https://github.com/neon1024/gen-ai-in-swe-project/issues/53) | US-46 Check in manually and correct attendance | open | Final | #50 |
| [#54](https://github.com/neon1024/gen-ai-in-swe-project/issues/54) | US-47 Give feedback | open | Final | #50 |
| [#55](https://github.com/neon1024/gen-ai-in-swe-project/issues/55) | US-48 View feedback | open | Final | #50 |
| [#56](https://github.com/neon1024/gen-ai-in-swe-project/issues/56) | US-49 View the event report | open | Final | #50 |
| [#57](https://github.com/neon1024/gen-ai-in-swe-project/issues/57) | US-50 Export the event report | open | Final | #50 |

After the user configures authentication on their own initiative, possible read-only commands from the root are:

```bash
rtk proxy gh repo view --json nameWithOwner,url,defaultBranchRef
rtk proxy gh issue list --state all --limit 300 --json number,title,state,body,labels,milestone,assignees,url
rtk proxy gh api --paginate 'repos/neon1024/gen-ai-in-swe-project/issues?state=all&per_page=100'
```

Do not automatically run `gh auth login` or store tokens in the repository. The public fallback completed this analysis without authentication.
