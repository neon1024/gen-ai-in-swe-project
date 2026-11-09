# Requirements: classification and coverage

Date: 2026-11-10. **Confirmed**: team-approved product baseline, confirmed by the user (D-002); **Unknown**: user validation; **Accepted technical direction**: ADR-001–008 by requesting-student authorization; **Proposed**: detailed slice contracts and technical task sequence. Product details are team decisions refining R1–R6, not literal assignment text. Keep existing IDs. Sources S1–S4: [archive](../sources/README.md).

## Product and acceptance

The [baseline](02-requirements.md) contains 51 FR and 25 BR; the [50 stories](08-user-stories.md) define verifiable criteria, including negative scenarios. The [original matrix](07-traceability.md) links FR/BR → UC/US/WF. The [development extension](traceability.md) adds tasks, components, dependencies, validation strategy and requirement status without copying normative text.

Obligation: every FR belongs to the team baseline, distributed across Mid-term and Final; Final does not mean optional. All are **not implemented**, based on code inspection. Only a Spring scaffold and demonstration endpoint exist. GitHub state was inspected separately; an open issue alone is insufficient proof of implementation or its absence.

## Non-functional requirements, constraints and deliverables

The IDs below belong to this Git baseline; they neither import nor renumber historical IDs from `Project/docs/`. Sources and locators identify explicit obligations; criteria/tests are proposed operational interpretations without additional quantitative targets. TC dependencies reflect existing technology, not a course mandate to use that technology.

| ID | Category / description | Source and locator | Obligation | Verifiable acceptance / validation | Dependencies | Status |
|---|---|---|---|---|---|---|
| NFR-001 | Authenticated and authorized access to the correct context | S1 R2; current roles; BR-03/18/19 | Mandatory | Permission matrix tested for allowed/denied actions and across organizations; no personal email exposed | FR-ACC-01–03; Q-02/Q-04 | Not implemented |
| NFR-002 | Persistent information and a model representing relationships and rules | S1 R7 | Mandatory | Account, organization, event and signup data recovered after restart; constraint/transaction tests | TC-002; Q-02 | Not implemented |
| NFR-003 | Separate presentation, business logic, persistence and interfaces | S1 R8 | Mandatory | Architecture/code review, justified decisions and testable boundaries | Q-01; TC-001 | Not implemented (scaffold only) |
| NFR-004 | Well-defined, documented APIs consistent with architecture | S1 R9 | Mandatory | Specification matches endpoints/errors; slice contract tests | NFR-003 | Not implemented |
| NFR-005 | Usable UI validated for principal workflows | S1 R10; S2 C; S4 Requirements & UX | Mandatory | UI supports documented flows; actual validation notes/improvements, wireframes and interviews | Q-02/Q-03; DEL-002 | Not implemented |
| NFR-006 | Automated tests for important behavior, including unit tests | S1 R11; S4 Testing & Quality | Mandatory | Executed test evidence linked to US/BR and justified integration levels | Implemented slice | Partial: one contextLoads, no product rules |
| NFR-007 | Runnable outside an individual's environment, with documented and demonstrated deployment | S1 R12; S4 Deployment & Operability | Mandatory | Another environment follows instructions and executes workflows; reproducible configuration and demo evidence | Q-10; TC-002/003 | Not implemented |
| TC-001 | Preserve existing stack until an explicit decision: Java 21/Spring Boot 4.1.1/Maven wrapper | backend/pom.xml; .mvn/wrapper/maven-wrapper.properties | Continuity constraint; not a course mandate | Build/test with compatible environment and declared versions; justify any change | Q-02 | Execution not verified |
| TC-002 | Integrate frontend, backend and database | S1 §1.1/R7–R10; S2 A; S4 Implementation & Functionality | Mandatory | Complete UI flow writes and reads persistent data through the backend | Q-01/Q-02 | Partial: initial backend only |
| TC-003 | Public repository and configured development environment at mid-term | S2 A | Mandatory | Accessible public link and reproducible frontend/backend/database instructions | TC-001/002 | Partial: public repository observed; setup incomplete |
| DEL-001 | Mid-term technical document: problem, scope, requirements, actors, UC and workflows | S2 B | Mandatory mid-term | Cross-document review and recorded human approval | D-002; DEL-002 | Partial: baseline exists; delivery review pending |
| DEL-002 | Real interviews, results, changed assumptions and UX validation | S2 C; S1 R10; S4 Requirements & UX | Mandatory | Anonymized notes, findings linked to hypotheses/requirements, documented changes and validation | Human recruitment; Q-03 | Not implemented; empty tables |
| DEL-003 | Mid-term GenAI declaration and final usage/reflection report in table format | S1 R13–R14; S2 D; S3 D | Mandatory | Known tools/models, objectives, artifacts, verification, human intervention, effectiveness/limitations | Per-session evidence | Partial: Phase 0 record, not semester report |
| DEL-004 | Each student owns a complete cross-layer workflow and an additional artifact | S1 R16; S4 Individual workflow/artifact | Mandatory | Approved names/responsibilities; each student demonstrates/explains the workflow and artifact | Q-09 | Not verified; historical proposal without names |
| DEL-005 | Individual feature implemented without GenAI | S4 Independent Feature / No-GenAI Implementation | Mandatory individual | Instructor-defined scope, student authorship/defense and evidence of no GenAI in the defined scope | Q-09 | Not implemented; agent must not generate it |
| DEL-006 | Mid-term individual declaration and final report in table format | S2 E; S3 E; S4 Contribution & Professionalism | Mandatory | Each student records actual functionality, artifacts, tests, decisions and GenAI use | DEL-004 | Not implemented |
| DEL-007 | Complete application and final document: architecture, technologies, testing and deployment | S3 A/B; S1 R7–R12 | Mandatory final | Mandatory workflows demonstrated; updated, consistent instructions and evidence | All FR; NFR-001–007 | Not implemented |
| DEL-008 | Trace requirements to workflows, decisions, implementation and tests | S1 R15; S3 B | Mandatory | Each FR/NFR/TC/DEL has a task and actual evidence; implementation/test links added when available | All requirements | Partial: traceable documentation; product code/tests absent |
| DEL-009 | Demonstrable human responsibility, technical understanding and collaboration | S1 R14/R16; S4 Technical Understanding/Contribution | Mandatory | Actual reviews, human decisions and verifiable contributions; individual defense, not commit counting | DEL-003/004/006 | Partial: human clarification D-002; defense pending |

## Evaluation

S4: team 50% — Requirements & UX 8%, Architecture & Design 10%, Implementation & Functionality 15%, Testing & Quality 7%, Deployment & Operability 5%, Documentation & Technical Communication 5%. Individual 50% — workflow 20%, additional artifact 10%, no-GenAI feature 10%, defense 5%, contribution 5%. Weights guide evidence, not additional features.

## Optional features, exclusions and boundaries

No additional optional feature was identified as a separate commitment. Optional fields (speaker, comment, online link depending on location, task due date/activity) remain subject to the corresponding story criteria. Payments, university SSO and additional external services are not requirements. Preserve explicit [scope](01-problem-and-scope.md) exclusions: venue booking, UBB-only participation, event approval, global statistics, following organizations/tags/free-places filter, certificates, walk-in, notification preferences and competition teams.

There are no documented targets for latency, uptime, load, coverage percentage or a formal accessibility standard. Safe concurrency, email retry policy, secure logging and keyboard navigation are design/validation recommendations to agree on, not invented official requirements. Do not confuse S1 R5's conditional examples with detailed obligations: the team selected tasks, communication and attendance, whose justification with users remains pending. The English translation preserves these meanings; the original approved product requirements and acceptance criteria remain unchanged.
