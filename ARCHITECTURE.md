# Architecture

## Overview

```
Flutter apps / Admin panel
        │  HTTPS (REST, /api/v1)   +   STOMP over WebSocket (/ws)
        ▼
┌───────────────────────────────────────────────┐
│ Spring Boot 4 (modular monolith)              │
│  security  → JWT filter, method security      │
│  module/*  → controller → service → repository│
│  common    → envelope, errors, base entities  │
└───────┬───────────────┬──────────────┬────────┘
     MySQL 8.4        Redis 7      Stripe / PayPal / Firebase / SMTP / S3
```

Principles

- **Backend is the source of truth.** Clients never contain business rules.
- **UTC everywhere.** `DATETIME` columns hold UTC; the salon's IANA timezone converts local
  opening hours and shifts.
- **Money in minor units.** `BIGINT` cents + ISO-4217 currency, never floating point.
- **Multi-tenant by `salon_id`.** Every salon-scoped table carries `salon_id`; services enforce
  that a `SALON_OWNER` only touches their own salons.
- **Schema owned by Flyway.** Hibernate runs with `ddl-auto: validate`.

## Package layout

```
com.slotify
├── SlotifyApplication          entry point, enables auditing / scheduling / properties
├── config/                     AppProperties, Security, CORS, OpenAPI, I18n, JPA (Clock)
├── common/
│   ├── api/                    ApiResponse, ApiError, PageResponse envelopes
│   ├── entity/                 BaseEntity, SoftDeletableEntity
│   └── exception/              ErrorCode, AppException, GlobalExceptionHandler
├── security/                   JsonAuthErrorHandlers (401/403 envelope); JWT filter (Phase 1)
└── module/
    ├── system/                 instance info endpoint (reference example)
    ├── auth/  user/  salon/  service/  staff/  booking/  payment/
    ├── promotion/  review/  notification/  customer/  report/  settings/
    ├── audit/                  append-only audit trail (AuditService.record from any service)
    └── <module>/
        ├── controller/         REST endpoints, validation, @PreAuthorize
        ├── service/            business logic, transactions
        ├── repository/         Spring Data JPA
        ├── entity/             JPA entities (extend BaseEntity)
        ├── dto/                request/response records
        └── mapper/             MapStruct entity <-> DTO
```

## Request flow

1. `JwtAuthenticationFilter` reads `Authorization: Bearer <token>`, validates it and puts a
   `UserPrincipal` into the security context.
2. The controller validates the request body (`@Valid`), checks the role (`@PreAuthorize`) and
   delegates to a service.
3. The service applies business rules inside a transaction and throws `AppException` with an
   `ErrorCode` on failure.
4. `GlobalExceptionHandler` converts exceptions to `ApiResponse` with the right HTTP status and a
   localised message (`Accept-Language`).
5. Successful responses are always wrapped: `ApiResponse.ok(dto)` or `PageResponse` for lists.

## Adding a new module

1. Create `module/<name>/{controller,service,repository,entity,dto,mapper}`.
2. Add a Flyway migration `V<n>__<description>.sql` in `src/main/resources/db/migration`.
3. Entities extend `BaseEntity` (or `SoftDeletableEntity`) – never add `id`/timestamps manually.
4. Add new error codes to `ErrorCode` and their messages to every `i18n/messages*.properties`.
5. Document endpoints with `@Tag` / `@Operation`; the admin panel regenerates its client from
   `/v3/api-docs`.
6. Write a unit test for the service and, for critical flows, an integration test importing
   `TestcontainersConfiguration`.

## Conventions

- Java `record` for DTOs and configuration; Lombok only on entities/services.
- Inject `java.time.Clock` instead of calling `Instant.now()` so tests can freeze time.
- No magic strings: enums for statuses, constants for Redis keys and topics.
- Formatting is enforced by Spotless (google-java-format); run `./mvnw spotless:apply`.

## Pinned versions

| Component            | Version |
| -------------------- | ------- |
| Java                 | 21      |
| Spring Boot          | 4.1.1   |
| springdoc-openapi    | 3.1.0   |
| MapStruct            | 1.6.3   |
| Bucket4j             | 8.19.0  |
| MySQL                | 8.4     |
| Redis                | 7       |
| Spotless plugin      | 3.10.0  |
| Checkstyle plugin    | 3.6.0   |

Check for updates with `./mvnw versions:display-dependency-updates`.
