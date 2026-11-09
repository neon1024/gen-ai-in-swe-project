# gen-ai-in-swe-project

Event management platform for student organizations at Babeș-Bolyai University (UBB), developed for the GenAI in Software Engineering project.

This Git root is the only development root. Status on 2026-11-10: product requirements approved by the team, user research pending, and an initial backend with `GET /` returning `Hello`. Product workflows are not implemented.

- [Agent instructions](AGENTS.md), [student/agent collaboration](docs/planning/collaborative-development.md) and [primary GenAI usage log](docs/genai_usage_log.md)
- [Existing requirements](docs/requirements/README.md) and [academic and technical supplement](docs/requirements/overview.md)
- [Development traceability](docs/requirements/traceability.md)
- [Accepted architecture direction](docs/architecture/overview.md) and [pending decisions](docs/decisions/README.md)
- [Roadmap](docs/planning/roadmap.md), [backlog](docs/planning/backlog.md) and [issue analysis](docs/planning/github-issues-analysis.md)
- [Sequential development prompts](docs/prompts/00-development-guide.md)
- [Phase 0 audit and validation](docs/planning/phase-0-audit.md)
- [Preserved academic sources](docs/sources/README.md)

## Existing backend

Declared stack: Java 21, Spring Boot 4.1.1, Maven 3.9.16 through the wrapper. From this root:

```bash
rtk proxy bash backend/mvnw -f backend/pom.xml test
rtk proxy bash backend/mvnw -f backend/pom.xml verify
rtk proxy bash backend/mvnw -f backend/pom.xml spring-boot:run
```

These commands were identified in existing files; execution has not been validated during this preparation. The wrapper may download tools/dependencies. Frontend, database, pipeline, lint and deployment are not configured. The requesting student authorized the technical direction in ADR-001–008; independent peer review and detailed slice approval remain required before implementation. Selected frontend/database/security tools are not installed by this documentation baseline. All repository artifacts must be in English; direct conversation with students may use European Portuguese.

## Collaborative kickoff

See [baseline and kickoff](docs/planning/baseline-and-kickoff.md) for authorized technical decisions, execution prerequisites and two independent prompts. The requesting student authorized the shared preparation commit/push after validation; application implementation is not part of this task.
