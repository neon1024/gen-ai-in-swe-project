# gen-ai-in-swe-project

Event management platform for student organizations at Babeș-Bolyai University (UBB), developed for the GenAI in Software Engineering project.

This Git root is the only development root. Status on 2026-11-18: accounts, organization membership, event authoring, discovery, lifecycle, feedback and reports are integrated (Spring Boot API + React SPA + PostgreSQL); participant registration and organization registration are not implemented yet. User research is pending. See the [mid-term demo script](docs/demo/mid-term-demo.md).

- [Agent instructions](AGENTS.md), [student/agent collaboration](docs/planning/collaborative-development.md) and [primary GenAI usage log](docs/genai_usage_log.md)
- [Existing requirements](docs/requirements/README.md) and [academic and technical supplement](docs/requirements/overview.md)
- [Development traceability](docs/requirements/traceability.md)
- [Accepted architecture direction](docs/architecture/overview.md) and [pending decisions](docs/decisions/README.md)
- [Roadmap](docs/planning/roadmap.md), [backlog](docs/planning/backlog.md) and [issue analysis](docs/planning/github-issues-analysis.md)
- [Sequential development prompts](docs/prompts/00-development-guide.md)
- [Phase 0 audit and validation](docs/planning/phase-0-audit.md)
- [Preserved academic sources](docs/sources/README.md)

## Run, test and deploy

Requirements: Java 21, Node.js 22 and Docker. From this root:

| Goal | Command |
|---|---|
| Develop: PostgreSQL and Mailpit in Docker, backend and Vite dev server (http://localhost:5173); emails land in http://localhost:8025 | `scripts/run-app.sh` (`--reset` wipes the local database and inbox) |
| Run the CI checks locally (backend `verify`, frontend lint, test and build) | `scripts/ci.sh` (`--images` also builds the Docker images) |
| Run the production images (http://localhost:8081) | `docker compose up --build` |
| Same, with the local demo accounts | `APP_FLYWAY_DEMO_LOCATION=",classpath:db/demo" docker compose up --build` |

GitHub Actions ([`ci-cd.yml`](.github/workflows/ci-cd.yml)) runs the same checks on every pull request and push. After both pass on `main`, it publishes `ghcr.io/neon1024/gen-ai-in-swe-project-backend` and `-frontend` (tags `latest` and the commit SHA). In the frontend image, nginx serves the SPA and proxies `/api` to the backend, so the session cookie and CSRF token stay same-origin. Configure the backend with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FRONTEND_URL` and, for verification emails, `RESEND_API_KEY`/`RESEND_FROM_EMAIL`. Never set `APP_FLYWAY_DEMO_LOCATION` outside local development.

## Collaborative kickoff

See [baseline and kickoff](docs/planning/baseline-and-kickoff.md) for authorized technical decisions, execution prerequisites and two independent prompts. The requesting student authorized the shared preparation commit/push after validation; application implementation is not part of this task.
