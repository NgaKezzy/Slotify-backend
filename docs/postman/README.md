# Postman collection

The Postman collection is generated from the live OpenAPI document, so it never drifts from
the code. It is not committed on every change; regenerate it whenever you need a fresh copy.

## Generate

```bash
./mvnw spring-boot:run            # API on http://localhost:8080
scripts/export-postman.sh         # writes docs/postman/slotify.postman_collection.json
```

Requirements: Node.js 18+ (`npx` downloads `openapi-to-postmanv2` on first use).
Point the script at another instance with `API_BASE_URL=https://api.example.com`.

## Use

1. Import `slotify.postman_collection.json` into Postman.
2. Create an environment with `baseUrl` (e.g. `http://localhost:8080`) and `accessToken`.
3. Call `POST /api/v1/auth/login`, copy `data.accessToken` into the `accessToken` variable;
   every protected request sends it as `Authorization: Bearer {{accessToken}}`.

Every response uses the `ApiResponse` envelope (`success`, `code`, `message`, `data`, `errors`);
branch on the numeric `code` (`1000` = success), never on the localised `message`.
