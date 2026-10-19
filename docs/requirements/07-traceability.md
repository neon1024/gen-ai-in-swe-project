# Traceability

Links each requirement and business rule to the use cases, user stories and workflows that realize it.

Implementation and test status are tracked on GitHub:
- each workflow is a parent issue, and each user story is a sub-issue with milestone **Mid-term** or **Final**;
- pull requests reference and close user-story issues;
- test names include the user story or business rule they verify (e.g. `US-27`, `BR-12`).

## Functional requirements

| Requirement | Use cases | User stories | Workflow | Milestone |
|---|---|---|---|---|
| FR-ACC-01 | UC-01 | US-01 | WF-01 | Mid-term |
| FR-ACC-02 | UC-01 | US-01 | WF-01 | Mid-term |
| FR-ACC-03 | UC-02 | US-02 | WF-01 | Mid-term |
| FR-ACC-04 | UC-31 | US-11 | WF-01 | Final |
| FR-ACC-05 | UC-31 | US-12 | WF-01 | Final |
| FR-ORG-01 | UC-03 | US-03 | WF-01 | Mid-term |
| FR-ORG-02 | UC-04 | US-04 | WF-01 | Mid-term |
| FR-ORG-03 | UC-05 | US-05 | WF-01 | Mid-term |
| FR-ORG-04 | UC-06, UC-07 | US-06, US-07 | WF-01 | Mid-term |
| FR-ORG-05 | UC-20, UC-22 | US-32, US-35 | WF-04 | Final |
| FR-ORG-06 | UC-08 | US-08, US-09 | WF-01 | Mid-term |
| FR-ORG-07 | UC-09 | US-10 | WF-01 | Mid-term |
| FR-ORG-08 | UC-31 | US-13 | WF-01 | Final |
| FR-EVT-01 | UC-10 | US-14 | WF-02 | Mid-term |
| FR-EVT-02 | UC-10 | US-15 | WF-02 | Mid-term |
| FR-EVT-03 | UC-10 | US-16 | WF-02 | Mid-term |
| FR-EVT-04 | UC-11 | US-17 | WF-02 | Mid-term |
| FR-EVT-05 | UC-12 | US-18 | WF-02 | Mid-term |
| FR-EVT-06 | UC-13 | US-20, US-21 | WF-02 | Mid-term |
| FR-EVT-07 | UC-14 | US-22 | WF-02 | Mid-term |
| FR-EVT-08 | UC-19 | US-23 | WF-02 | Mid-term |
| FR-DSC-01 | UC-15 | US-24 | WF-03 | Mid-term |
| FR-DSC-02 | UC-15 | US-24 | WF-03 | Mid-term |
| FR-DSC-03 | UC-15 | US-25 | WF-03 | Mid-term |
| FR-DSC-04 | UC-15 | US-26 | WF-03 | Mid-term |
| FR-DSC-05 | UC-18 | US-31 | WF-03 | Mid-term |
| FR-REG-01 | UC-16 | US-27 | WF-03 | Mid-term |
| FR-REG-02 | UC-16 | US-27 | WF-03 | Mid-term |
| FR-REG-03 | UC-17 | US-28 | WF-03 | Mid-term |
| FR-REG-04 | UC-17 | US-29 | WF-03 | Mid-term |
| FR-REG-05 | UC-16 | US-27 | WF-03 | Mid-term |
| FR-PRV-01 | UC-21 | US-33 | WF-04 | Final |
| FR-PRV-02 | UC-21 | US-34 | WF-04 | Final |
| FR-PRV-03 | UC-22 | US-35 | WF-04 | Final |
| FR-NTF-01 | UC-23 | US-36 | WF-04 | Final |
| FR-NTF-02 | UC-23 | US-37 | WF-04 | Final |
| FR-NTF-03 | UC-12, UC-13 | US-19 | WF-02 | Final |
| FR-NTF-04 | UC-17 | US-30 | WF-03 | Final |
| FR-NTF-05 | UC-24 | US-42 | WF-05 | Final |
| FR-NTF-06 | UC-25 | US-43 | WF-05 | Final |
| FR-TEAM-01 | UC-26 | US-38 | WF-05 | Final |
| FR-TEAM-02 | UC-26 | US-39 | WF-05 | Final |
| FR-TEAM-03 | UC-27 | US-40 | WF-05 | Final |
| FR-TEAM-04 | UC-27 | US-41 | WF-05 | Final |
| FR-ATT-01 | UC-28 | US-44 | WF-06 | Final |
| FR-ATT-02 | UC-28 | US-45 | WF-06 | Final |
| FR-ATT-03 | UC-28 | US-46 | WF-06 | Final |
| FR-FBK-01 | UC-29 | US-47 | WF-06 | Final |
| FR-FBK-02 | UC-30 | US-48 | WF-06 | Final |
| FR-RPT-01 | UC-30 | US-49 | WF-06 | Final |
| FR-RPT-02 | UC-30 | US-50 | WF-06 | Final |

## Business rules

| Rule | Requirements | User stories |
|---|---|---|
| BR-01 | FR-EVT-04, FR-EVT-06, FR-EVT-08 | US-17, US-21, US-23 |
| BR-02 | FR-EVT-04 | US-17 |
| BR-03 | FR-EVT-01, FR-DSC-01 | US-14, US-24 |
| BR-04 | FR-EVT-06, FR-EVT-08 | US-20, US-23 |
| BR-05 | FR-EVT-06, FR-EVT-07 | US-21, US-22 |
| BR-06 | FR-EVT-08 | US-23 |
| BR-07 | FR-EVT-02, FR-EVT-03 | US-15, US-16 |
| BR-08 | FR-EVT-02 | US-15 |
| BR-09 | FR-DSC-04, FR-REG-01 | US-26, US-27 |
| BR-10 | FR-REG-01, FR-PRV-03 | US-27, US-35 |
| BR-11 | FR-REG-05, FR-PRV-03 | US-27, US-35 |
| BR-12 | FR-REG-04, FR-EVT-05, FR-ACC-04 | US-11, US-18, US-29 |
| BR-13 | FR-REG-03 | US-28 |
| BR-14 | FR-EVT-05, FR-EVT-06 | US-18, US-20 |
| BR-15 | FR-ATT-02, FR-ATT-03 | US-45, US-46 |
| BR-16 | FR-FBK-01 | US-47 |
| BR-17 | FR-FBK-02 | US-48 |
| BR-18 | FR-DSC-01, FR-REG-01, FR-PRV-01 | US-24, US-27, US-33 |
| BR-19 | FR-ORG-05, FR-PRV-02, FR-NTF-02 | US-32, US-34, US-37 |
| BR-20 | FR-ACC-02 | US-01 |
| BR-21 | FR-ORG-06, FR-ORG-07 | US-08, US-09, US-10 |
| BR-22 | FR-ORG-06, FR-ORG-07, FR-TEAM-01 | US-09, US-10, US-38 |
| BR-23 | FR-ORG-06, FR-ORG-07, FR-TEAM-02 | US-09, US-10, US-39 |
| BR-24 | FR-ORG-08 | US-13 |
| BR-25 | FR-ACC-05 | US-12 |
