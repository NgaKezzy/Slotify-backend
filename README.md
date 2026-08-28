# Slotify Backend

REST API for **Slotify** – a multi-salon booking & management platform for spas and salons.
Powers the Customer app, the Staff app (Flutter) and the Admin Panel (Next.js).

- Spring Boot 4 · Java 21 · MySQL 8.4 · Redis 7 · MinIO (S3)
- JWT authentication (email/password + Google Sign-In)
- Real-time updates over STOMP WebSocket, push notifications via Firebase
- Online payments with Stripe and PayPal
- Swagger UI at `/swagger-ui.html`

There is **one environment**: everything machine-specific (ports, credentials,
whether to seed demo data) lives in `.env`. No Spring profiles to choose.

## Requirements

| Tool           | Version                           |
| -------------- | --------------------------------- |
| JDK            | 21                                |
| Docker Desktop | 24+ (MySQL, Redis, MinIO, MailHog) |
| Maven          | bundled (`./mvnw` / `mvnw.cmd`)   |

## Setup

### Option A – one command (recommended on a fresh machine)

```bash
# macOS / Linux
./scripts/setup.sh          # writes .env + slotify-admin/.env.local, starts the Docker stack
./scripts/setup.sh --run    # ...and starts the API on http://localhost:8081
```

```powershell
# Windows (PowerShell)
.\scripts\setup.ps1
.\scripts\setup.ps1 -Run
```

The script checks Docker (and tells you how to install it if missing), creates
the MinIO bucket and configures **one login for everything**: user `admin`,
password `admin123`. Override with `ADMIN_USER` / `ADMIN_PASSWORD` (or the
individual variables `DB_*`, `S3_*`, `DEMO_ADMIN_*`, `SERVER_PORT`) exported
before running.

| Service                | URL                             | Login                          |
| ---------------------- | ------------------------------- | ------------------------------ |
| API                    | http://localhost:8081           | –                              |
| MySQL                  | localhost:3306, db `slotify`    | `admin` / `admin123` (root: `admin123`) |
| MinIO console          | http://localhost:9001           | `admin` / `admin123`           |
| MailHog (caught email) | http://localhost:8025           | `admin` / `admin123`           |
| Admin panel / apps     | see the other repositories      | `admin@admin.com` / `admin123` |

Notes: MySQL cannot create a user named `root`, so the application user is
`admin` (root exists too, same password); app/web logins must be email
addresses, hence `admin@admin.com`; MinIO needs passwords of 8+ characters.
The MailHog login is a bcrypt hash in `docker/mailhog-auth` (regenerate with
`docker exec slotify-mailhog MailHog bcrypt <password>`).

### Option B – manual

```bash
cp .env.example .env          # edit if needed (defaults below)
docker compose up -d          # MySQL, Redis, MinIO (+ bucket init), MailHog
./mvnw spring-boot:run        # reads .env, API on http://localhost:8081
```

`.env.example` carries the same defaults as the script (`admin` / `admin123`
everywhere, platform admin `admin@admin.com`).

Run everything in Docker instead:

```bash
docker compose --profile api up --build
```

## Default accounts (demo data)

Demo data is inserted on the first start while `SEED_DEMO_DATA=true` (the
default). All demo accounts share `DEMO_ADMIN_PASSWORD` (`admin123`):

| Account                 | Role        | Used in                          |
| ----------------------- | ----------- | -------------------------------- |
| `admin@admin.com`       | SUPER_ADMIN | Admin panel → Platform area      |
| `owner@slotify.demo`    | SALON_OWNER | Admin panel (Glow & Go, Serenity Spa) |
| `staff@slotify.demo`    | STAFF       | Staff app (Sam Stylist, Glow & Go) |
| `customer@slotify.demo` | CUSTOMER    | Customer app                     |

Set `SEED_DEMO_DATA=false` for an empty database.

## Useful URLs

- Swagger UI: <http://localhost:8081/swagger-ui.html> (OpenAPI JSON at `/v3/api-docs`)
- Health: <http://localhost:8081/actuator/health>
- MailHog: <http://localhost:8025> · MinIO console: <http://localhost:9001>

## Useful commands

```bash
./mvnw verify                                   # format check + checkstyle + tests (needs Docker)
./mvnw spotless:apply                           # auto-format sources
./mvnw versions:display-dependency-updates      # list newer library versions
./mvnw clean package -DskipTests                # build target/slotify-backend-*.jar
docker build -t slotify-backend .               # production image
scripts/export-postman.sh                       # Postman collection from a running instance
```

## Configuration

All settings are environment variables read from `.env`; see
[`.env.example`](.env.example) for the full, commented list. The most important ones:

| Variable                     | Description                                         |
| ---------------------------- | --------------------------------------------------- |
| `SERVER_PORT`                | API port (default `8081`)                           |
| `SEED_DEMO_DATA`             | Insert demo salons/accounts on first start          |
| `DEMO_ADMIN_EMAIL/PASSWORD`  | Seeded platform-admin login                         |
| `LOG_LEVEL`                  | Log level of `com.slotify` (`DEBUG` for verbose)    |
| `JWT_SECRET`                 | HMAC secret for signing tokens (64+ chars)          |
| `DB_*`, `REDIS_*`, `S3_*`    | Database, cache and object-storage connections      |
| `CORS_ALLOWED_ORIGINS`       | Admin panel origin(s)                               |
| `STRIPE_*`, `PAYPAL_*`       | Payment providers                                   |
| `GOOGLE_CLIENT_ID`           | Verifies Google Sign-In ID tokens                   |
| `FIREBASE_CREDENTIALS_PATH`  | Service-account JSON for push (default `firebase-service-account.json`) |

## Push notifications (Firebase)

The code is complete on both ends; you only need to paste a key:

1. [Firebase console](https://console.firebase.google.com) → your project →
   ⚙️ *Project settings* → *Service accounts* → **Generate new private key**.
2. Save the downloaded JSON as `slotify-backend/firebase-service-account.json`
   (git-ignored; `FIREBASE_CREDENTIALS_PATH` in `.env` already points there).
3. Restart the API – the log shows `Firebase Cloud Messaging initialised`.
   Without the file it logs a warning and runs with push disabled.
4. Test: sign in on the mobile app → *Profile → Notifications → Send test
   notification*, or call `POST /api/v1/me/push-test` from Swagger. The
   response tells you whether credentials are loaded, how many devices the
   account has and how many received the push.

Pushes are sent for every in-app notification (new / confirmed / cancelled /
rescheduled bookings, payments, reviews, reminders). The super admin can also
send free-text **announcements** to customers, staff or everyone from the
admin panel (*Platform → Announcements*, `POST /admin/platform/notifications/broadcast`). The mobile apps must use
the **same Firebase project** (see the mobile README); iOS additionally needs an
APNs key uploaded in Firebase → *Cloud Messaging*.

## Project layout

See [ARCHITECTURE.md](ARCHITECTURE.md) for the package structure, request flow and how to add
a new module. API conventions: [`docs/api/README.md`](docs/api/README.md).

## Support

Questions or issues: **ngakezzy@gmail.com**

## License

Commercial – distributed through CodeCanyon. See the item license for terms.
