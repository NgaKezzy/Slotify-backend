#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# One-shot environment bootstrap for a fresh macOS / Linux machine.
# (Windows: scripts\setup.ps1)
#
#   ./scripts/setup.sh          # prepare .env files + start Docker stack
#   ./scripts/setup.sh --run    # ...and then start the API
#
# What it does
#   1. Checks Docker (prints install instructions when missing).
#   2. Writes slotify-backend/.env from .env.example with the credentials below.
#   3. Starts MySQL, Redis, MinIO (+ bucket), MailHog via docker-compose.
#   4. Writes slotify-admin/.env.local pointing at the API port.
#
# Credentials: one user name / password for everything (override by exporting
# ADMIN_USER / ADMIN_PASSWORD, or the individual variables, before running):
#   MySQL      $ADMIN_USER / $ADMIN_PASSWORD      (root uses the same password)
#   MinIO      $ADMIN_USER / $ADMIN_PASSWORD      (MinIO needs >= 8 characters)
#   MailHog    admin / admin123                   (web UI; see docker/mailhog-auth)
#   Admin web  $ADMIN_USER@admin.com / $ADMIN_PASSWORD (login requires an email)
# ---------------------------------------------------------------------------
set -euo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADMIN_DIR="$(cd "$BACKEND_DIR/.." && pwd)/slotify-admin"

SERVER_PORT="${SERVER_PORT:-8081}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
DB_USER="${DB_USER:-$ADMIN_USER}"
DB_PASSWORD="${DB_PASSWORD:-$ADMIN_PASSWORD}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-$ADMIN_PASSWORD}"
S3_ACCESS_KEY="${S3_ACCESS_KEY:-$ADMIN_USER}"
S3_SECRET_KEY="${S3_SECRET_KEY:-$ADMIN_PASSWORD}"
DEMO_ADMIN_EMAIL="${DEMO_ADMIN_EMAIL:-$ADMIN_USER@admin.com}"
DEMO_ADMIN_PASSWORD="${DEMO_ADMIN_PASSWORD:-$ADMIN_PASSWORD}"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mWARN\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31mERROR\033[0m %s\n' "$*" >&2; exit 1; }

# --- 1. Docker ---------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  case "$(uname -s)" in
    Darwin)
      die "Docker is not installed. Install Docker Desktop: brew install --cask docker  (then open Docker.app), or https://docs.docker.com/desktop/install/mac-install/" ;;
    Linux)
      die "Docker is not installed. Run: curl -fsSL https://get.docker.com | sh && sudo usermod -aG docker \$USER  (log out/in afterwards)" ;;
    *)
      die "Docker is not installed. See https://docs.docker.com/get-docker/" ;;
  esac
fi
docker info >/dev/null 2>&1 || die "Docker daemon is not running. Start Docker Desktop and retry."
docker compose version >/dev/null 2>&1 || die "Docker Compose v2 is required (docker compose)."

if [ "${#S3_SECRET_KEY}" -lt 8 ]; then
  die "MinIO requires a secret key of at least 8 characters (got '$S3_SECRET_KEY')."
fi
if [ "$DB_USER" = "root" ]; then
  die "DB_USER must not be root (the MySQL image cannot create it); root still works with DB_ROOT_PASSWORD."
fi

# --- 2. Backend .env -----------------------------------------------------------
cd "$BACKEND_DIR"
if [ -f .env ]; then
  cp .env ".env.bak.$(date +%Y%m%d%H%M%S)"
  warn "Existing .env backed up."
fi
cp .env.example .env

# set KEY VALUE: replace the line KEY=... in .env (or append when absent).
set_env() {
  local key="$1" value="$2"
  if grep -q "^${key}=" .env; then
    # '|' as the sed delimiter: values contain '/' (URLs).
    sed -i.tmp "s|^${key}=.*|${key}=${value}|" .env && rm -f .env.tmp
  else
    printf '%s=%s\n' "$key" "$value" >> .env
  fi
}
set_env SERVER_PORT "$SERVER_PORT"
set_env SEED_DEMO_DATA true
set_env APP_BASE_URL "http://localhost:${SERVER_PORT}"
set_env DB_USER "$DB_USER"
set_env DB_PASSWORD "$DB_PASSWORD"
set_env DB_ROOT_PASSWORD "$DB_ROOT_PASSWORD"
set_env MYSQL_APP_USER "$DB_USER"
set_env MYSQL_APP_PASSWORD "$DB_PASSWORD"
set_env S3_ACCESS_KEY "$S3_ACCESS_KEY"
set_env S3_SECRET_KEY "$S3_SECRET_KEY"
set_env DEMO_ADMIN_EMAIL "$DEMO_ADMIN_EMAIL"
set_env DEMO_ADMIN_PASSWORD "$DEMO_ADMIN_PASSWORD"
set_env JWT_SECRET "$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 80)"
info "Wrote $BACKEND_DIR/.env"

# --- 3. Docker stack -----------------------------------------------------------
info "Starting MySQL, Redis, MinIO, MailHog..."
docker compose up -d
info "Waiting for MySQL to be healthy..."
for _ in $(seq 1 60); do
  status="$(docker inspect -f '{{.State.Health.Status}}' slotify-mysql 2>/dev/null || echo starting)"
  [ "$status" = "healthy" ] && break
  sleep 2
done
[ "$status" = "healthy" ] || die "MySQL did not become healthy; check: docker compose logs mysql"
docker compose up -d minio-init >/dev/null
info "Docker stack is up."

# --- 4. Admin web .env.local ---------------------------------------------------
if [ -d "$ADMIN_DIR" ]; then
  cat > "$ADMIN_DIR/.env.local" <<ENV
NEXT_PUBLIC_API_URL=http://localhost:${SERVER_PORT}
API_URL=http://localhost:${SERVER_PORT}
NEXT_PUBLIC_WS_URL=ws://localhost:${SERVER_PORT}/ws
AUTH_SECRET=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 48)
AUTH_URL=http://localhost:3000
ENV
  info "Wrote $ADMIN_DIR/.env.local"
else
  warn "slotify-admin not found next to slotify-backend; skipped its .env.local"
fi

# --- Summary -------------------------------------------------------------------
cat <<SUMMARY

Ready. Credentials:
  MySQL       localhost:3306  db=slotify  ${DB_USER} / ${DB_PASSWORD}  (root / ${DB_ROOT_PASSWORD})
  MinIO       http://localhost:9001 (console)  ${S3_ACCESS_KEY} / ${S3_SECRET_KEY}
  MailHog     http://localhost:8025  admin / admin123
  Admin web   http://localhost:3000  ${DEMO_ADMIN_EMAIL} / ${DEMO_ADMIN_PASSWORD}
  Other demo  owner@ / staff@ / customer@slotify.demo, password ${DEMO_ADMIN_PASSWORD}

Start the API (reads .env, seeds demo data on first run):
  cd $BACKEND_DIR && ./mvnw spring-boot:run
Start the admin web:
  cd $ADMIN_DIR && pnpm install && pnpm dev
SUMMARY

# --- Optional: run the API now ------------------------------------------------
if [ "${1:-}" = "--run" ]; then
  info "Starting the API on port ${SERVER_PORT}..."
  exec ./mvnw spring-boot:run
fi
