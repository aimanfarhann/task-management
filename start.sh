#!/usr/bin/env bash
#
# TaskFlow dev launcher — one command for the whole native dev stack:
#   1. PostgreSQL in Docker
#   2. Spring Boot backend (dev profile)  -> http://localhost:8080
#   3. Angular dev server                 -> http://localhost:4200
#
# Backend + frontend logs stream together; press Ctrl+C to stop them.
# PostgreSQL is left running (run `docker compose stop postgres` to stop it too).
#
# Usage:  ./start.sh        (Git Bash on Windows, or any bash shell)

set -uo pipefail
cd "$(dirname "$0")"

die() { echo "ERROR: $*" >&2; exit 1; }

# --- Prerequisites -----------------------------------------------------------
command -v docker >/dev/null 2>&1 || die "Docker is not installed or not on PATH."
docker info >/dev/null 2>&1 || die "Docker daemon is not running — start Docker Desktop."
command -v npm >/dev/null 2>&1 || die "Node.js / npm is not installed or not on PATH."

# Java isn't always on PATH (e.g. Windows). Fall back to JAVA_HOME, then a common
# Temurin install location; the Maven wrapper uses JAVA_HOME when set.
if ! command -v java >/dev/null 2>&1 && [ -z "${JAVA_HOME:-}" ]; then
  for candidate in "/c/Program Files/Eclipse Adoptium"/jdk-21* "/c/Program Files/Java"/jdk-21*; do
    [ -d "$candidate" ] && export JAVA_HOME="$candidate" && break
  done
fi
[ -n "${JAVA_HOME:-}" ] && export PATH="$JAVA_HOME/bin:$PATH"
command -v java >/dev/null 2>&1 || die "Java 21 not found. Install JDK 21 or set JAVA_HOME."

# --- Environment -------------------------------------------------------------
if [ ! -f .env ]; then
  echo "==> No .env found — creating one from .env.example with a generated JWT_SECRET."
  cp .env.example .env
  secret="$(openssl rand -hex 64 2>/dev/null || head -c 64 /dev/urandom | od -An -tx1 | tr -d ' \n')"
  sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${secret}|" .env
fi
set -a; . ./.env; set +a
[ -n "${JWT_SECRET:-}" ] || die "JWT_SECRET is empty in .env — set it (openssl rand -hex 64)."
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_PASSWORD="${POSTGRES_PASSWORD:-taskflow}"

# --- PostgreSQL --------------------------------------------------------------
echo "==> Starting PostgreSQL..."
docker compose up -d postgres || die "Failed to start PostgreSQL."
printf "    waiting for it to be healthy"
tries=0
until [ "$(docker inspect --format '{{.State.Health.Status}}' taskflow-postgres-1 2>/dev/null)" = "healthy" ]; do
  tries=$((tries + 1))
  [ "$tries" -ge 30 ] && { echo; die "PostgreSQL did not become healthy in time."; }
  printf "."; sleep 2
done
echo " ready."

# --- Frontend dependencies (first run only) ----------------------------------
if [ ! -d frontend/node_modules ]; then
  echo "==> Installing frontend dependencies (first run — slow once)..."
  ( cd frontend && npm install ) || die "npm install failed."
fi

# --- Backend + frontend ------------------------------------------------------
pids=""
cleanup() {
  echo
  echo "==> Stopping backend + frontend (PostgreSQL stays up: 'docker compose stop postgres')."
  # shellcheck disable=SC2086
  kill $pids 2>/dev/null || true
}
trap cleanup INT TERM

echo "==> Starting backend  -> http://localhost:8080"
( cd backend && exec ./mvnw spring-boot:run ) &
pids="$pids $!"

echo "==> Starting frontend -> http://localhost:4200"
( cd frontend && exec npm start ) &
pids="$pids $!"

echo "==> Up. Press Ctrl+C to stop backend + frontend."
wait
