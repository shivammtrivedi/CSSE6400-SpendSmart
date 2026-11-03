# Expense Service

Expense and settlement API for SpendSmart.

## Tech Stack
- Java 21
- Spring Boot 3
- Spring Data JPA (PostgreSQL)
- Spring Security OAuth2 Resource Server (JWT)
- Springdoc OpenAPI + Swagger UI

## Running Locally
1. Start PostgreSQL (or use the provided compose stack at repo root).
2. Set datasource env vars if not using defaults:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
3. Run:
```bash
./gradlew bootRun
```

Default port: `8082`

## API Documentation
- OpenAPI JSON: `http://localhost:8082/api-docs`
- Swagger UI: `http://localhost:8082/swagger-ui.html`

## Authentication
- All non-health endpoints require a Bearer JWT.
- Header:
```http
Authorization: Bearer <token>
```

## Implemented Endpoints

### Health
- `GET /api/v1/expense/health`

### Expense
- `POST /api/v1/expense`
  - Creates expense + shares.
- `GET /api/v1/expense/{groupId}`
  - Lists group expenses (active member required).
- `GET /api/v1/expense/item/{expenseId}`
  - Gets one expense by id (active member required).
- `PUT /api/v1/expense/{expenseId}`
  - Updates expense and split (owner + version required).
- `DELETE /api/v1/expense/{expenseId}`
  - Deletes expense (owner + version required).

### Settlement
- `GET /api/v1/expense/{expenseId}/settlements`
  - Lists settlements (caller must be included in expense share).
- `POST /api/v1/expense/{expenseId}/settlements`
  - Creates settlement link (caller must be included, version checked).
- `DELETE /api/v1/expense/{expenseId}/settlements/{linkId}`
  - Deletes settlement (owner + version required).

## Request/Response Shapes

### `POST /api/v1/expense` request
```json
{
  "groupId": "uuid",
  "amount": 120.50,
  "split": [
    { "userId": "uuid", "amount": 60.25 },
    { "userId": "uuid", "amount": 60.25 }
  ]
}
```

### `PUT /api/v1/expense/{expenseId}` request
```json
{
  "groupId": "uuid",
  "amount": 150.00,
  "version": 2,
  "split": [
    { "userId": "uuid", "amount": 75.00 },
    { "userId": "uuid", "amount": 75.00 }
  ]
}
```

### `POST /api/v1/expense/{expenseId}/settlements` request
```json
{
  "version": 2,
  "provider": "obp",
  "transactionId": "external-txn-id",
  "amount": 25.00
}
```

## Error Model
Global handler returns:
```json
{
  "code": "BAD_REQUEST | VALIDATION_ERROR | FORBIDDEN | NOT_FOUND | CONFLICT | INTERNAL_ERROR",
  "message": "details",
  "requestId": "uuid"
}
```

## Concurrency Notes
- `Expense` and `Settlement` use optimistic locking (`@Version`).
- Write operations also precheck request `version` and return `409 CONFLICT` on mismatch.
