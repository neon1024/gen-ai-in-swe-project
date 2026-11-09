# Development traceability

Extension of the [original matrix](07-traceability.md), without replacing descriptions or criteria. FR sources: [02-requirements.md](02-requirements.md), the corresponding ID row (with R1–R6); acceptance: referenced US sections in [08-user-stories.md](08-user-stories.md), including every checkbox. **Confirmed**: approved baseline (D-002), all 51 FR and 25 BR awaiting implementation. **Proposed**: components, tasks, dependencies and future tests. No future test path is presented as an existing file.

Priority: the source's Mid-term or Final milestone, both mandatory in the baseline. Dependencies: each T-US predecessor in the [backlog](../planning/backlog.md) plus the relevant M decisions in the [roadmap](../planning/roadmap.md). Per-row strategy: automate associated US/BR criteria, rule tests and API/UI integration, authorization denial and persistence; task-specific exceptions/risks are in the task. Actual product implementation/test evidence: absent in every row.

| FR / source | UC / WF | Acceptance US | Milestone | Primary issues / aliases / dependencies | Proposed components | Proposed validation | Status |
|---|---|---|---|---|---|---|---|
| FR-ACC-01 (02, row FR-ACC-01) | UC-01; WF-01 | US-01 | Mid-term | [#3](https://github.com/neon1024/gen-ai-in-swe-project/issues/3) (T-US-01); predecessors in backlog | Identity/UI | Criteria US-01, associated rules and integration | Not implemented |
| FR-ACC-02 (02, row FR-ACC-02) | UC-01; WF-01 | US-01 | Mid-term | [#3](https://github.com/neon1024/gen-ai-in-swe-project/issues/3) (T-US-01); predecessors in backlog | Identity/UI | Criteria US-01, associated rules and integration | Not implemented |
| FR-ACC-03 (02, row FR-ACC-03) | UC-02; WF-01 | US-02 | Mid-term | [#4](https://github.com/neon1024/gen-ai-in-swe-project/issues/4) (T-US-02); predecessors in backlog | Identity/UI | Criteria US-02, associated rules and integration | Not implemented |
| FR-ACC-04 (02, row FR-ACC-04) | UC-31; WF-01 | US-11 | Final | [#13](https://github.com/neon1024/gen-ai-in-swe-project/issues/13) (T-US-11); predecessors in backlog | Identity/UI | Criteria US-11, associated rules and integration | Not implemented |
| FR-ACC-05 (02, row FR-ACC-05) | UC-31; WF-01 | US-12 | Final | [#14](https://github.com/neon1024/gen-ai-in-swe-project/issues/14) (T-US-12); predecessors in backlog | Identity/UI | Criteria US-12, associated rules and integration | Not implemented |
| FR-ORG-01 (02, row FR-ORG-01) | UC-03; WF-01 | US-03 | Mid-term | [#5](https://github.com/neon1024/gen-ai-in-swe-project/issues/5) (T-US-03); predecessors in backlog | Organizations/UI | Criteria US-03, associated rules and integration | Not implemented |
| FR-ORG-02 (02, row FR-ORG-02) | UC-04; WF-01 | US-04 | Mid-term | [#6](https://github.com/neon1024/gen-ai-in-swe-project/issues/6) (T-US-04); predecessors in backlog | Organizations/UI | Criteria US-04, associated rules and integration | Not implemented |
| FR-ORG-03 (02, row FR-ORG-03) | UC-05; WF-01 | US-05 | Mid-term | [#7](https://github.com/neon1024/gen-ai-in-swe-project/issues/7) (T-US-05); predecessors in backlog | Organizations/UI | Criteria US-05, associated rules and integration | Not implemented |
| FR-ORG-04 (02, row FR-ORG-04) | UC-06, UC-07; WF-01 | US-06, US-07 | Mid-term | [#8](https://github.com/neon1024/gen-ai-in-swe-project/issues/8) (T-US-06), [#9](https://github.com/neon1024/gen-ai-in-swe-project/issues/9) (T-US-07); predecessors in backlog | Organizations/UI | Criteria US-06, US-07, associated rules and integration | Not implemented |
| FR-ORG-05 (02, row FR-ORG-05) | UC-20, UC-22; WF-04 | US-32, US-35 | Final | [#37](https://github.com/neon1024/gen-ai-in-swe-project/issues/37) (T-US-32), [#40](https://github.com/neon1024/gen-ai-in-swe-project/issues/40) (T-US-35); predecessors in backlog | Communication/invitations/UI | Criteria US-32, US-35, associated rules and integration | Not implemented |
| FR-ORG-06 (02, row FR-ORG-06) | UC-08; WF-01 | US-08, US-09 | Mid-term | [#10](https://github.com/neon1024/gen-ai-in-swe-project/issues/10) (T-US-08), [#11](https://github.com/neon1024/gen-ai-in-swe-project/issues/11) (T-US-09); predecessors in backlog | Organizations/UI | Criteria US-08, US-09, associated rules and integration | Not implemented |
| FR-ORG-07 (02, row FR-ORG-07) | UC-09; WF-01 | US-10 | Mid-term | [#12](https://github.com/neon1024/gen-ai-in-swe-project/issues/12) (T-US-10); predecessors in backlog | Organizations/UI | Criteria US-10, associated rules and integration | Not implemented |
| FR-ORG-08 (02, row FR-ORG-08) | UC-31; WF-01 | US-13 | Final | [#15](https://github.com/neon1024/gen-ai-in-swe-project/issues/15) (T-US-13); predecessors in backlog | Organizations/UI | Criteria US-13, associated rules and integration | Not implemented |
| FR-EVT-01 (02, row FR-EVT-01) | UC-10; WF-02 | US-14 | Mid-term | [#17](https://github.com/neon1024/gen-ai-in-swe-project/issues/17) (T-US-14); predecessors in backlog | Events/UI/clock | Criteria US-14, associated rules and integration | Not implemented |
| FR-EVT-02 (02, row FR-EVT-02) | UC-10; WF-02 | US-15 | Mid-term | [#18](https://github.com/neon1024/gen-ai-in-swe-project/issues/18) (T-US-15); predecessors in backlog | Events/UI/clock | Criteria US-15, associated rules and integration | Not implemented |
| FR-EVT-03 (02, row FR-EVT-03) | UC-10; WF-02 | US-16 | Mid-term | [#19](https://github.com/neon1024/gen-ai-in-swe-project/issues/19) (T-US-16); predecessors in backlog | Events/UI/clock | Criteria US-16, associated rules and integration | Not implemented |
| FR-EVT-04 (02, row FR-EVT-04) | UC-11; WF-02 | US-17 | Mid-term | [#20](https://github.com/neon1024/gen-ai-in-swe-project/issues/20) (T-US-17); predecessors in backlog | Events/UI/clock | Criteria US-17, associated rules and integration | Not implemented |
| FR-EVT-05 (02, row FR-EVT-05) | UC-12; WF-02 | US-18 | Mid-term | [#21](https://github.com/neon1024/gen-ai-in-swe-project/issues/21) (T-US-18); predecessors in backlog | Events/UI/clock | Criteria US-18, associated rules and integration | Not implemented |
| FR-EVT-06 (02, row FR-EVT-06) | UC-13; WF-02 | US-20, US-21 | Mid-term | [#23](https://github.com/neon1024/gen-ai-in-swe-project/issues/23) (T-US-20), [#24](https://github.com/neon1024/gen-ai-in-swe-project/issues/24) (T-US-21); predecessors in backlog | Events/UI/clock | Criteria US-20, US-21, associated rules and integration | Not implemented |
| FR-EVT-07 (02, row FR-EVT-07) | UC-14; WF-02 | US-22 | Mid-term | [#25](https://github.com/neon1024/gen-ai-in-swe-project/issues/25) (T-US-22); predecessors in backlog | Events/UI/clock | Criteria US-22, associated rules and integration | Not implemented |
| FR-EVT-08 (02, row FR-EVT-08) | UC-19; WF-02 | US-23 | Mid-term | [#26](https://github.com/neon1024/gen-ai-in-swe-project/issues/26) (T-US-23); predecessors in backlog | Events/UI/clock | Criteria US-23, associated rules and integration | Not implemented |
| FR-DSC-01 (02, row FR-DSC-01) | UC-15; WF-03 | US-24 | Mid-term | [#28](https://github.com/neon1024/gen-ai-in-swe-project/issues/28) (T-US-24); predecessors in backlog | Discovery/UI | Criteria US-24, associated rules and integration | Not implemented |
| FR-DSC-02 (02, row FR-DSC-02) | UC-15; WF-03 | US-24 | Mid-term | [#28](https://github.com/neon1024/gen-ai-in-swe-project/issues/28) (T-US-24); predecessors in backlog | Discovery/UI | Criteria US-24, associated rules and integration | Not implemented |
| FR-DSC-03 (02, row FR-DSC-03) | UC-15; WF-03 | US-25 | Mid-term | [#29](https://github.com/neon1024/gen-ai-in-swe-project/issues/29) (T-US-25); predecessors in backlog | Discovery/UI | Criteria US-25, associated rules and integration | Not implemented |
| FR-DSC-04 (02, row FR-DSC-04) | UC-15; WF-03 | US-26 | Mid-term | [#30](https://github.com/neon1024/gen-ai-in-swe-project/issues/30) (T-US-26); predecessors in backlog | Discovery/UI | Criteria US-26, associated rules and integration | Not implemented |
| FR-DSC-05 (02, row FR-DSC-05) | UC-18; WF-03 | US-31 | Mid-term | [#35](https://github.com/neon1024/gen-ai-in-swe-project/issues/35) (T-US-31); predecessors in backlog | Registrations/UI | Criteria US-31, associated rules and integration | Not implemented |
| FR-REG-01 (02, row FR-REG-01) | UC-16; WF-03 | US-27 | Mid-term | [#31](https://github.com/neon1024/gen-ai-in-swe-project/issues/31) (T-US-27); predecessors in backlog | Registrations/UI | Criteria US-27, associated rules and integration | Not implemented |
| FR-REG-02 (02, row FR-REG-02) | UC-16; WF-03 | US-27 | Mid-term | [#31](https://github.com/neon1024/gen-ai-in-swe-project/issues/31) (T-US-27); predecessors in backlog | Registrations/UI | Criteria US-27, associated rules and integration | Not implemented |
| FR-REG-03 (02, row FR-REG-03) | UC-17; WF-03 | US-28 | Mid-term | [#32](https://github.com/neon1024/gen-ai-in-swe-project/issues/32) (T-US-28); predecessors in backlog | Registrations/UI | Criteria US-28, associated rules and integration | Not implemented |
| FR-REG-04 (02, row FR-REG-04) | UC-17; WF-03 | US-29 | Mid-term | [#33](https://github.com/neon1024/gen-ai-in-swe-project/issues/33) (T-US-29); predecessors in backlog | Registrations/UI | Criteria US-29, associated rules and integration | Not implemented |
| FR-REG-05 (02, row FR-REG-05) | UC-16; WF-03 | US-27 | Mid-term | [#31](https://github.com/neon1024/gen-ai-in-swe-project/issues/31) (T-US-27); predecessors in backlog | Registrations/UI | Criteria US-27, associated rules and integration | Not implemented |
| FR-PRV-01 (02, row FR-PRV-01) | UC-21; WF-04 | US-33 | Final | [#38](https://github.com/neon1024/gen-ai-in-swe-project/issues/38) (T-US-33); predecessors in backlog | Communication/invitations/UI | Criteria US-33, associated rules and integration | Not implemented |
| FR-PRV-02 (02, row FR-PRV-02) | UC-21; WF-04 | US-34 | Final | [#39](https://github.com/neon1024/gen-ai-in-swe-project/issues/39) (T-US-34); predecessors in backlog | Communication/invitations/UI | Criteria US-34, associated rules and integration | Not implemented |
| FR-PRV-03 (02, row FR-PRV-03) | UC-22; WF-04 | US-35 | Final | [#40](https://github.com/neon1024/gen-ai-in-swe-project/issues/40) (T-US-35); predecessors in backlog | Communication/invitations/UI | Criteria US-35, associated rules and integration | Not implemented |
| FR-NTF-01 (02, row FR-NTF-01) | UC-23; WF-04 | US-36 | Final | [#41](https://github.com/neon1024/gen-ai-in-swe-project/issues/41) (T-US-36); predecessors in backlog | Communication/invitations/UI | Criteria US-36, associated rules and integration | Not implemented |
| FR-NTF-02 (02, row FR-NTF-02) | UC-23; WF-04 | US-37 | Final | [#42](https://github.com/neon1024/gen-ai-in-swe-project/issues/42) (T-US-37); predecessors in backlog | Communication/invitations/UI | Criteria US-37, associated rules and integration | Not implemented |
| FR-NTF-03 (02, row FR-NTF-03) | UC-12, UC-13; WF-02 | US-19 | Final | [#22](https://github.com/neon1024/gen-ai-in-swe-project/issues/22) (T-US-19); predecessors in backlog | Communication/invitations/UI | Criteria US-19, associated rules and integration | Not implemented |
| FR-NTF-04 (02, row FR-NTF-04) | UC-17; WF-03 | US-30 | Final | [#34](https://github.com/neon1024/gen-ai-in-swe-project/issues/34) (T-US-30); predecessors in backlog | Communication/invitations/UI | Criteria US-30, associated rules and integration | Not implemented |
| FR-NTF-05 (02, row FR-NTF-05) | UC-24; WF-05 | US-42 | Final | [#48](https://github.com/neon1024/gen-ai-in-swe-project/issues/48) (T-US-42); predecessors in backlog | Communication/invitations/UI | Criteria US-42, associated rules and integration | Not implemented |
| FR-NTF-06 (02, row FR-NTF-06) | UC-25; WF-05 | US-43 | Final | [#49](https://github.com/neon1024/gen-ai-in-swe-project/issues/49) (T-US-43); predecessors in backlog | Communication/invitations/UI | Criteria US-43, associated rules and integration | Not implemented |
| FR-TEAM-01 (02, row FR-TEAM-01) | UC-26; WF-05 | US-38 | Final | [#44](https://github.com/neon1024/gen-ai-in-swe-project/issues/44) (T-US-38); predecessors in backlog | Team/tasks/UI | Criteria US-38, associated rules and integration | Not implemented |
| FR-TEAM-02 (02, row FR-TEAM-02) | UC-26; WF-05 | US-39 | Final | [#45](https://github.com/neon1024/gen-ai-in-swe-project/issues/45) (T-US-39); predecessors in backlog | Team/tasks/UI | Criteria US-39, associated rules and integration | Not implemented |
| FR-TEAM-03 (02, row FR-TEAM-03) | UC-27; WF-05 | US-40 | Final | [#46](https://github.com/neon1024/gen-ai-in-swe-project/issues/46) (T-US-40); predecessors in backlog | Team/tasks/UI | Criteria US-40, associated rules and integration | Not implemented |
| FR-TEAM-04 (02, row FR-TEAM-04) | UC-27; WF-05 | US-41 | Final | [#47](https://github.com/neon1024/gen-ai-in-swe-project/issues/47) (T-US-41); predecessors in backlog | Team/tasks/UI | Criteria US-41, associated rules and integration | Not implemented |
| FR-ATT-01 (02, row FR-ATT-01) | UC-28; WF-06 | US-44 | Final | [#51](https://github.com/neon1024/gen-ai-in-swe-project/issues/51) (T-US-44); predecessors in backlog | Attendance/UI | Criteria US-44, associated rules and integration | Not implemented |
| FR-ATT-02 (02, row FR-ATT-02) | UC-28; WF-06 | US-45 | Final | [#52](https://github.com/neon1024/gen-ai-in-swe-project/issues/52) (T-US-45); predecessors in backlog | Attendance/UI | Criteria US-45, associated rules and integration | Not implemented |
| FR-ATT-03 (02, row FR-ATT-03) | UC-28; WF-06 | US-46 | Final | [#53](https://github.com/neon1024/gen-ai-in-swe-project/issues/53) (T-US-46); predecessors in backlog | Attendance/UI | Criteria US-46, associated rules and integration | Not implemented |
| FR-FBK-01 (02, row FR-FBK-01) | UC-29; WF-06 | US-47 | Final | [#54](https://github.com/neon1024/gen-ai-in-swe-project/issues/54) (T-US-47); predecessors in backlog | Feedback/reports/UI | Criteria US-47, associated rules and integration | Not implemented |
| FR-FBK-02 (02, row FR-FBK-02) | UC-30; WF-06 | US-48 | Final | [#55](https://github.com/neon1024/gen-ai-in-swe-project/issues/55) (T-US-48); predecessors in backlog | Feedback/reports/UI | Criteria US-48, associated rules and integration | Not implemented |
| FR-RPT-01 (02, row FR-RPT-01) | UC-30; WF-06 | US-49 | Final | [#56](https://github.com/neon1024/gen-ai-in-swe-project/issues/56) (T-US-49); predecessors in backlog | Feedback/reports/UI | Criteria US-49, associated rules and integration | Not implemented |
| FR-RPT-02 (02, row FR-RPT-02) | UC-30; WF-06 | US-50 | Final | [#57](https://github.com/neon1024/gen-ai-in-swe-project/issues/57) (T-US-50); predecessors in backlog | Feedback/reports/UI | Criteria US-50, associated rules and integration | Not implemented |

## Business rules

Source: `02-requirements.md`, Business rules section, corresponding ID row; mandatory in the baseline. Each task below inherits dependencies and components from its referenced stories. Validate the complete rule text and boundaries, including rejected cases.

| BR / source | Related FR | Acceptance US | Tasks | Strategy | Status |
|---|---|---|---|---|---|
| BR-01 (02, row BR-01) | FR-EVT-04, FR-EVT-06, FR-EVT-08 | US-17, US-21, US-23 | [#20](https://github.com/neon1024/gen-ai-in-swe-project/issues/20) (T-US-17), [#24](https://github.com/neon1024/gen-ai-in-swe-project/issues/24) (T-US-21), [#26](https://github.com/neon1024/gen-ai-in-swe-project/issues/26) (T-US-23) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-02 (02, row BR-02) | FR-EVT-04 | US-17 | [#20](https://github.com/neon1024/gen-ai-in-swe-project/issues/20) (T-US-17) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-03 (02, row BR-03) | FR-EVT-01, FR-DSC-01 | US-14, US-24 | [#17](https://github.com/neon1024/gen-ai-in-swe-project/issues/17) (T-US-14), [#28](https://github.com/neon1024/gen-ai-in-swe-project/issues/28) (T-US-24) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-04 (02, row BR-04) | FR-EVT-06, FR-EVT-08 | US-20, US-23 | [#23](https://github.com/neon1024/gen-ai-in-swe-project/issues/23) (T-US-20), [#26](https://github.com/neon1024/gen-ai-in-swe-project/issues/26) (T-US-23) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-05 (02, row BR-05) | FR-EVT-06, FR-EVT-07 | US-21, US-22 | [#24](https://github.com/neon1024/gen-ai-in-swe-project/issues/24) (T-US-21), [#25](https://github.com/neon1024/gen-ai-in-swe-project/issues/25) (T-US-22) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-06 (02, row BR-06) | FR-EVT-08 | US-23 | [#26](https://github.com/neon1024/gen-ai-in-swe-project/issues/26) (T-US-23) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-07 (02, row BR-07) | FR-EVT-02, FR-EVT-03 | US-15, US-16 | [#18](https://github.com/neon1024/gen-ai-in-swe-project/issues/18) (T-US-15), [#19](https://github.com/neon1024/gen-ai-in-swe-project/issues/19) (T-US-16) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-08 (02, row BR-08) | FR-EVT-02 | US-15 | [#18](https://github.com/neon1024/gen-ai-in-swe-project/issues/18) (T-US-15) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-09 (02, row BR-09) | FR-DSC-04, FR-REG-01 | US-26, US-27 | [#30](https://github.com/neon1024/gen-ai-in-swe-project/issues/30) (T-US-26), [#31](https://github.com/neon1024/gen-ai-in-swe-project/issues/31) (T-US-27) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-10 (02, row BR-10) | FR-REG-01, FR-PRV-03 | US-27, US-35 | [#31](https://github.com/neon1024/gen-ai-in-swe-project/issues/31) (T-US-27), [#40](https://github.com/neon1024/gen-ai-in-swe-project/issues/40) (T-US-35) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-11 (02, row BR-11) | FR-REG-05, FR-PRV-03 | US-27, US-35 | [#31](https://github.com/neon1024/gen-ai-in-swe-project/issues/31) (T-US-27), [#40](https://github.com/neon1024/gen-ai-in-swe-project/issues/40) (T-US-35) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-12 (02, row BR-12) | FR-REG-04, FR-EVT-05, FR-ACC-04 | US-11, US-18, US-29 | [#13](https://github.com/neon1024/gen-ai-in-swe-project/issues/13) (T-US-11), [#21](https://github.com/neon1024/gen-ai-in-swe-project/issues/21) (T-US-18), [#33](https://github.com/neon1024/gen-ai-in-swe-project/issues/33) (T-US-29) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-13 (02, row BR-13) | FR-REG-03 | US-28 | [#32](https://github.com/neon1024/gen-ai-in-swe-project/issues/32) (T-US-28) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-14 (02, row BR-14) | FR-EVT-05, FR-EVT-06 | US-18, US-20 | [#21](https://github.com/neon1024/gen-ai-in-swe-project/issues/21) (T-US-18), [#23](https://github.com/neon1024/gen-ai-in-swe-project/issues/23) (T-US-20) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-15 (02, row BR-15) | FR-ATT-02, FR-ATT-03 | US-45, US-46 | [#52](https://github.com/neon1024/gen-ai-in-swe-project/issues/52) (T-US-45), [#53](https://github.com/neon1024/gen-ai-in-swe-project/issues/53) (T-US-46) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-16 (02, row BR-16) | FR-FBK-01 | US-47 | [#54](https://github.com/neon1024/gen-ai-in-swe-project/issues/54) (T-US-47) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-17 (02, row BR-17) | FR-FBK-02 | US-48 | [#55](https://github.com/neon1024/gen-ai-in-swe-project/issues/55) (T-US-48) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-18 (02, row BR-18) | FR-DSC-01, FR-REG-01, FR-PRV-01 | US-24, US-27, US-33 | [#28](https://github.com/neon1024/gen-ai-in-swe-project/issues/28) (T-US-24), [#31](https://github.com/neon1024/gen-ai-in-swe-project/issues/31) (T-US-27), [#38](https://github.com/neon1024/gen-ai-in-swe-project/issues/38) (T-US-33) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-19 (02, row BR-19) | FR-ORG-05, FR-PRV-02, FR-NTF-02 | US-32, US-34, US-37 | [#37](https://github.com/neon1024/gen-ai-in-swe-project/issues/37) (T-US-32), [#39](https://github.com/neon1024/gen-ai-in-swe-project/issues/39) (T-US-34), [#42](https://github.com/neon1024/gen-ai-in-swe-project/issues/42) (T-US-37) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-20 (02, row BR-20) | FR-ACC-02 | US-01 | [#3](https://github.com/neon1024/gen-ai-in-swe-project/issues/3) (T-US-01) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-21 (02, row BR-21) | FR-ORG-06, FR-ORG-07 | US-08, US-09, US-10 | [#10](https://github.com/neon1024/gen-ai-in-swe-project/issues/10) (T-US-08), [#11](https://github.com/neon1024/gen-ai-in-swe-project/issues/11) (T-US-09), [#12](https://github.com/neon1024/gen-ai-in-swe-project/issues/12) (T-US-10) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-22 (02, row BR-22) | FR-ORG-06, FR-ORG-07, FR-TEAM-01 | US-09, US-10, US-38 | [#11](https://github.com/neon1024/gen-ai-in-swe-project/issues/11) (T-US-09), [#12](https://github.com/neon1024/gen-ai-in-swe-project/issues/12) (T-US-10), [#44](https://github.com/neon1024/gen-ai-in-swe-project/issues/44) (T-US-38) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-23 (02, row BR-23) | FR-ORG-06, FR-ORG-07, FR-TEAM-02 | US-09, US-10, US-39 | [#11](https://github.com/neon1024/gen-ai-in-swe-project/issues/11) (T-US-09), [#12](https://github.com/neon1024/gen-ai-in-swe-project/issues/12) (T-US-10), [#45](https://github.com/neon1024/gen-ai-in-swe-project/issues/45) (T-US-39) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-24 (02, row BR-24) | FR-ORG-08 | US-13 | [#15](https://github.com/neon1024/gen-ai-in-swe-project/issues/15) (T-US-13) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |
| BR-25 (02, row BR-25) | FR-ACC-05 | US-12 | [#14](https://github.com/neon1024/gen-ai-in-swe-project/issues/14) (T-US-12) | Unit rule + story integration; time/concurrency tests where applicable | Not implemented |

## Cross-cutting obligations

Descriptions, exact sources, priorities, acceptance, dependencies and status are in the [overview](overview.md); this table adds tasks/components and validation.

| Requirement | Increments / tasks | Components | Validation / future evidence |
|---|---|---|---|
| NFR-001 | M2–M7; all 50 story issues (aliases T-US-01 to T-US-50); P-06/P-07 | Identity and every context | Permission matrix, negative/cross-organization tests |
| NFR-002 | M1–M7; P-04/P-06/P-07; stories with data | Approved persistence | Restart, constraints and concurrent transactions |
| NFR-003 | M0–M7; P-01/P-05/P-07 | Layers and capabilities | Design/code review and decisions |
| NFR-004 | M1–M7; P-04/P-05/P-07 | API | Contracts and errors versus actual endpoints |
| NFR-005 | M0–M7; P-02/P-06/P-07 | UI and six workflows | Wireframes, interviews and validation with improvements |
| NFR-006 | M1–M7; P-04/P-06/P-07; every T-US | Tests | Automated results with story/rule IDs |
| NFR-007 | M1/M4/M7; P-04/P-06/P-07 | Deployment and configuration | Demonstrated execution outside the individual environment |
| TC-001 | M0/M1; P-01/P-04 | backend/ | Build/test with recorded versions and environment |
| TC-002 | M1–M7; P-04/P-06/P-07 | UI/API/database | Persistent integrated workflows |
| TC-003 | M1/M4; P-04/P-06 | Repository and setup | Public link and reproducible execution |
| DEL-001 | M0/M4; P-02/P-05/P-06 | Technical documentation | Reviewed S2 B package |
| DEL-002 | M0–M7; P-02/P-06/P-07 | Research/UX | Actual interviews, findings, changes and validation |
| DEL-003 | M0–M7; P-08/P-06/P-09 | GenAI reports | Mid-term declaration and final report/reflection |
| DEL-004 | M0–M7; P-03/P-09 | Team | Demonstrated workflow and additional artifact ownership |
| DEL-005 | M0/M7; P-03/P-09 | Human no-GenAI feature | Evidence and defense according to instructor guidance |
| DEL-006 | M4/M7; P-03/P-06/P-09 | Contributions | Accurate individual tables |
| DEL-007 | M7; P-05/P-07/P-09 | System/documentation | Complete, runnable S3 delivery |
| DEL-008 | M0–M7; P-05/P-06/P-07 | Matrix and actual code/tests | No orphan requirement; links added with evidence |
| DEL-009 | M0–M7; P-03/P-08/P-09 | Reviews and defense | Recorded human decisions and assessment |

## Assignment coverage R1–R16

| Source | Local coverage |
|---|---|
| R1 | FR-EVT-01–08 |
| R2 | FR-ACC-01–05; FR-ORG-01–08; NFR-001 |
| R3 | FR-REG-01–05; FR-DSC-05; FR-PRV-01; FR-NTF-04 |
| R4 | FR-EVT-04/06/08; FR-FBK-01/02; FR-ATT-01–03 as the team’s execution refinement |
| R5 | FR-PRV-02/03; FR-NTF-01–03/05/06; FR-TEAM-01–04; FR-ATT-01–03; FR-RPT-01/02 |
| R6 | FR-DSC-01–04 |
| R7 | NFR-002; TC-002 |
| R8 | NFR-003 |
| R9 | NFR-004 |
| R10 | NFR-005; DEL-002 |
| R11 | NFR-006 |
| R12 | NFR-007 |
| R13–R14 | DEL-003/009 |
| R15 | DEL-008 |
| R16 | DEL-004/006/009 |

Tracing a proposal does not constitute implementation or validation. Update actual decision/API/code/test links per task; keep this matrix and the product matrix consistent.

## Coordination and evidence per issue

The GitHub issue is the primary execution key. Human ownership, milestone, dependencies, branch, PR, status and verification are in the [backlog](../planning/backlog.md), using the Phase 0.1 query. T-US aliases are retained for Phase 0 continuity. For NFR/TC/DEL tied to P tasks without an issue, association with a real issue remains pending; do not create or invent numbers.

Each implementation links issue → FR/BR/US → ADR/contract → actual code/tests → PR → entry in the [GenAI log](../genai_usage_log.md). Record human approval/review only with confirmation; AI verification remains separately classified. This update does not claim implemented requirements. All saved traceability descriptions must be in English.

## Accepted technical decisions

The [ADR register](../decisions/README.md) records requesting-student authorization, with peer review pending: ADR-001/002 → NFR-003 and TC-001/002; ADR-003 → NFR-002 and BR-07–14; ADR-004 → NFR-001 and FR-ACC/ORG authorization; ADR-005 → NFR-004 and account/notification email; ADR-006 → NFR-005/006; ADR-007 → NFR-007 and TC-003; ADR-008 → DEL-003–006/009. These links do not mark any product requirement implemented or alter acceptance criteria. See [kickoff](../planning/baseline-and-kickoff.md) for issue execution gates.
