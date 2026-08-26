# Slotify Backend

REST API for **Slotify** – a multi-salon booking & management platform for spas and salons.
Powers the Customer app, the Staff app (Flutter) and the Admin Panel (Next.js).

- Spring Boot 4 · Java 21 · MySQL 8.4 · Redis 7
- JWT authentication (email/password + Google Sign-In)
- Real-time updates over STOMP WebSocket, push notifications via Firebase
- Online payments with Stripe and PayPal
- Swagger UI at `/swagger-ui.html`

## Requirements

| Tool           | Version |
| -------------- | ------- |
| JDK            | 21      |
| Docker Desktop | 24+     |
| Maven          | bundled (`./mvnw`) |

## Quick start (development)

```bash
cp .env.example .env          # adjust if needed
docker compose up -d          # MySQL, Redis, MinIO, MailHog
./mvnw spring-boot:run        # API on http://localhost:8080
```

Then open:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>
- MailHog (captured emails): <http://localhost:8025>
- MinIO console: <http://localhost:9001>

Run everything in Docker instead:

```bash
docker compose --profile api up --build
```

## Profiles

| Profile | Purpose                                                        |
| ------- | -------------------------------------------------------------- |
| `dev`   | Default. Verbose logging, SQL echo.                            |
| `demo`  | Like `dev` but seeds demo salons/bookings on startup.          |
| `prod`  | Quiet logging, devtools disabled. Set `SPRING_PROFILES_ACTIVE=prod`. |

## Useful commands

```bash
./mvnw verify                                   # format check + checkstyle + tests (needs Docker)
./mvnw spotless:apply                           # auto-format sources
./mvnw versions:display-dependency-updates      # list newer library versions
./mvnw clean package -DskipTests                # build target/slotify-backend-*.jar
docker build -t slotify-backend .               # production image
```

## Configuration

All settings are environment variables; see [`.env.example`](.env.example) for the full,
commented list. The most important ones:

| Variable                 | Description                                   |
| ------------------------ | --------------------------------------------- |
| `JWT_SECRET`             | HMAC secret for signing tokens (64+ chars)    |
| `DB_*`, `REDIS_*`        | Database and cache connection                 |
| `CORS_ALLOWED_ORIGINS`   | Admin panel origin(s)                         |
| `STRIPE_*`, `PAYPAL_*`   | Payment providers                             |
| `GOOGLE_CLIENT_ID`       | Verifies Google Sign-In ID tokens             |
| `FIREBASE_CREDENTIALS_PATH` | Service-account JSON for push notifications |

## Project layout

See [ARCHITECTURE.md](ARCHITECTURE.md) for the package structure, request flow and how to add
a new module.

## License

Commercial – distributed through CodeCanyon. See the item license for terms.
# Slotify-backend
