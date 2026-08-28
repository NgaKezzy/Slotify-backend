# API documentation

The API is documented from the code with springdoc-openapi; nothing here is hand-written.

| Resource        | URL (local development)                        |
| --------------- | ---------------------------------------------- |
| Swagger UI      | <http://localhost:8081/swagger-ui.html>        |
| OpenAPI (JSON)  | <http://localhost:8081/v3/api-docs>            |
| OpenAPI (YAML)  | <http://localhost:8081/v3/api-docs.yaml>       |
| Postman         | `scripts/export-postman.sh` → [`../postman`](../postman/README.md) |

Conventions worth knowing before reading the endpoints:

- Every response is wrapped in `ApiResponse` (`success`, `code`, `message`, `data`, `errors`).
  `code` is `1000` on success, otherwise a value of `ErrorCode`; `message` is localised through
  `Accept-Language` (en, de, fr, es, it, vi).
- Protected endpoints expect `Authorization: Bearer <accessToken>` obtained from
  `POST /api/v1/auth/login` (or `/auth/google`), refreshed through `POST /api/v1/auth/refresh`.
- `GET /api/v1/me/export` is the only endpoint that returns a raw file (JSON attachment) instead
  of the envelope.
