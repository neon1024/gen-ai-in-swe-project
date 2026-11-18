#!/usr/bin/env bash
# Runs the whole application locally: PostgreSQL (Docker), Spring Boot backend and Vite frontend.
#
# Usage:
#   scripts/run-app.sh          start everything; Ctrl+C stops backend and frontend
#   scripts/run-app.sh --stop   stop and remove the PostgreSQL container (data is kept in its volume)
#   scripts/run-app.sh --reset  remove the PostgreSQL container and its data volume
#
# Development only: the database password below is a local default, not a secret.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_CONTAINER="${DB_CONTAINER:-ubb-events-postgres}"
DB_VOLUME="${DB_CONTAINER}-data"
DB_PORT="${DB_PORT:-55432}"  # not 5432, to avoid clashing with other local databases
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:${DB_PORT}/ubb_events}"
export DB_USERNAME="${DB_USERNAME:-ubb}"
export DB_PASSWORD="${DB_PASSWORD:-ubb-local-dev}"
# Local demo accounts (db/demo, password DemoPass123!); never set this outside local development.
export APP_FLYWAY_DEMO_LOCATION=",classpath:db/demo"
BACKEND_PORT=8080
LOG_DIR="$ROOT/.cache/run-app"

log() { printf '\033[1;34m[run-app]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[run-app]\033[0m %s\n' "$*" >&2; exit 1; }

case "${1:-}" in
  --stop)
    docker rm -f "$DB_CONTAINER" >/dev/null 2>&1 && log "PostgreSQL stopped." || log "PostgreSQL was not running."
    exit 0 ;;
  --reset)
    docker rm -f "$DB_CONTAINER" >/dev/null 2>&1 || true
    docker volume rm "$DB_VOLUME" >/dev/null 2>&1 || true
    log "PostgreSQL container and data removed."
    exit 0 ;;
  "") ;;
  *) fail "Unknown option: $1 (use --stop or --reset)" ;;
esac

# --- Prerequisites ---------------------------------------------------------
command -v docker >/dev/null || fail "Docker is required."
docker info >/dev/null 2>&1 || fail "Docker is not running."
command -v npm >/dev/null || fail "Node.js/npm is required."
if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in /usr/lib/jvm/java-21-openjdk /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/temurin-21-jdk; do
    [[ -x "$candidate/bin/java" ]] && export JAVA_HOME="$candidate" && break
  done
fi
[[ -n "${JAVA_HOME:-}" ]] || fail "Java 21 not found; set JAVA_HOME."
if (exec 3<>/dev/tcp/127.0.0.1/$BACKEND_PORT) 2>/dev/null; then
  fail "Port $BACKEND_PORT is already in use; stop the other process first."
fi
mkdir -p "$LOG_DIR"

# --- PostgreSQL ------------------------------------------------------------
if docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
  log "PostgreSQL already running ($DB_CONTAINER)."
elif docker ps -a --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
  log "Starting existing PostgreSQL container..."
  docker start "$DB_CONTAINER" >/dev/null
else
  if (exec 3<>/dev/tcp/127.0.0.1/$DB_PORT) 2>/dev/null; then
    fail "Port $DB_PORT is already in use; rerun with DB_PORT=<free port>."
  fi
  log "Creating PostgreSQL container on port $DB_PORT..."
  trap 'docker rm -f "$DB_CONTAINER" >/dev/null 2>&1' ERR
  docker run -d --name "$DB_CONTAINER" \
    -e POSTGRES_DB=ubb_events -e POSTGRES_USER="$DB_USERNAME" -e POSTGRES_PASSWORD="$DB_PASSWORD" \
    -p "$DB_PORT:5432" -v "$DB_VOLUME:/var/lib/postgresql/data" postgres:17-alpine >/dev/null
  trap - ERR
fi
log "Waiting for PostgreSQL..."
for _ in $(seq 1 30); do
  docker exec "$DB_CONTAINER" pg_isready -U "$DB_USERNAME" -d ubb_events >/dev/null 2>&1 && break
  sleep 1
done
docker exec "$DB_CONTAINER" pg_isready -U "$DB_USERNAME" -d ubb_events >/dev/null 2>&1 || fail "PostgreSQL did not become ready."

# --- Backend and frontend --------------------------------------------------
PIDS=()
cleanup() {
  log "Stopping backend and frontend (PostgreSQL keeps running; use --stop to stop it)..."
  for pid in "${PIDS[@]}"; do kill "$pid" 2>/dev/null || true; done
  wait 2>/dev/null || true
}
trap cleanup EXIT INT TERM

log "Starting backend (log: $LOG_DIR/backend.log)..."
bash "$ROOT/backend/mvnw" -q -f "$ROOT/backend/pom.xml" spring-boot:run >"$LOG_DIR/backend.log" 2>&1 &
PIDS+=($!)

log "Waiting for backend on port $BACKEND_PORT (first run downloads dependencies)..."
for _ in $(seq 1 180); do
  kill -0 "${PIDS[0]}" 2>/dev/null || { tail -n 40 "$LOG_DIR/backend.log" >&2; fail "Backend exited; see the log above."; }
  curl -s -o /dev/null "http://localhost:$BACKEND_PORT/v3/api-docs" && break
  sleep 1
done
curl -s -o /dev/null "http://localhost:$BACKEND_PORT/v3/api-docs" || fail "Backend did not start in time; see $LOG_DIR/backend.log."

if [[ ! -d "$ROOT/frontend/node_modules" ]]; then
  log "Installing frontend dependencies..."
  (cd "$ROOT/frontend" && npm ci --no-audit --no-fund >/dev/null)
fi
log "Starting frontend (log: $LOG_DIR/frontend.log)..."
(cd "$ROOT/frontend" && exec npm run dev -- --port 5173 --strictPort) >"$LOG_DIR/frontend.log" 2>&1 &
PIDS+=($!)
sleep 3

log "Ready:"
log "  Frontend  http://localhost:5173/events"
log "  Backend   http://localhost:$BACKEND_PORT  (API docs: /v3/api-docs)"
log "Press Ctrl+C to stop."
wait -n "${PIDS[@]}" || true
log "A process exited; check the logs in $LOG_DIR."
