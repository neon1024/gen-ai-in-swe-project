#!/usr/bin/env bash
# Runs the same checks as .github/workflows/ci-cd.yml locally. Needs Java 21, Node.js 22 and Docker
# (backend tests start PostgreSQL with Testcontainers).
#
# Usage: scripts/ci.sh           backend verify + frontend lint, test and build
#        scripts/ci.sh --images  also build the Docker images the CD job publishes
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
log() { printf '\033[1;34m[ci]\033[0m %s\n' "$*"; }

if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in /usr/lib/jvm/java-21-openjdk /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/temurin-21-jdk; do
    [[ -x "$candidate/bin/java" ]] && export JAVA_HOME="$candidate" && break
  done
fi

log "Backend: mvnw verify"
bash "$ROOT/backend/mvnw" -B -f "$ROOT/backend/pom.xml" verify

log "Frontend: npm ci, lint, test, build"
(cd "$ROOT/frontend" && npm ci --no-audit --no-fund && npm run lint && npm test && npm run build)

if [[ "${1:-}" == "--images" ]]; then
  log "Docker images"
  docker build -t ubb-events-backend "$ROOT/backend"
  docker build -t ubb-events-frontend "$ROOT/frontend"
fi
log "All checks passed."
