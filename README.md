# tdb-vazhipadu-backend

A REST service for booking **vazhipadu** (temple offerings) at temples run by a temple board.

A vazhipadu is a ritual offering that a devotee pays the temple to perform, such as Pushpanjali or Archana. The devotee pays a fixed fee, and the priest performs the ritual, usually in the name of a person and their birth star (nakshatra), on a chosen date.

The service is built in four steps, one pull request each (see [Pull requests](#pull-requests)):

| Step | Feature |
|---|---|
| P1 | Offerings catalogue and orders, with the order total calculated on the server |
| P2 | Paged order search by date, status and phone number |
| P3 | Optional daily booking limit per offering that holds when orders arrive at the same moment |
| P4 | Order cancellation and a daily collection report in Indian Standard Time |

## Tech stack

| Area | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.6 (Web, Validation, Data JPA) |
| Database | H2, in memory by default |
| Migrations | Flyway |
| API docs | springdoc-openapi (Swagger UI) |
| Build | Maven 3.9 through the Maven wrapper (`mvnw`) |
| Tests | JUnit 5, Spring Boot Test, MockMvc |
| Coverage | JaCoCo; the build fails below 90% line or branch coverage |

## Prerequisites

- **JDK 21.** Check with `java -version`.
- **Nothing else.** Maven is not required because the wrapper downloads it on first use, and the database runs in memory.

## Run the app

```bash
git clone https://github.com/sharok23/tdb-vazhipadu-backend.git
cd tdb-vazhipadu-backend

./mvnw spring-boot:run          # macOS / Linux / Git Bash
mvnw.cmd spring-boot:run        # Windows cmd / PowerShell
```

The app starts on `http://localhost:8080`.

## Try the API in Swagger

While the app is running:

- Swagger UI: http://localhost:8080/swagger-ui.html — every endpoint, with a **Try it out** button to send real requests
- OpenAPI JSON: http://localhost:8080/v3/api-docs

The H2 database is in memory, so **all data is lost when the app stops**.

## Run the tests

```bash
./mvnw clean verify
```

This runs all tests and checks code coverage.

- **Coverage report:** open `target/site/jacoco/index.html` in a browser. The build fails if line or branch coverage drops below 90%.
- Test reports are written to `target/surefire-reports/`.
- To run one test class: `./mvnw test -Dtest=GlobalExceptionHandlerTest`.
- `ERROR ... Unhandled error` lines in the output are expected: some tests check the 500 path on purpose.

## Configuration

Settings live in `src/main/resources/application.properties`. You can override database settings with environment variables. All of them are optional.

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:h2:mem:vazhipadu;DB_CLOSE_DELAY=-1` | JDBC URL of the database |
| `DB_USERNAME` | `sa` | Database user |
| `DB_PASSWORD` | empty | Database password |

The HTTP port is `8080`; override it with `SERVER_PORT`. Never commit secrets: pass real credentials as environment variables.

## Project structure

Code is organised by layer under `com.tdb.vazhipadu`:

```
src/main/java/com/tdb/vazhipadu/
├── VazhipaduApplication.java   Spring Boot entry point
├── config/                     App configuration (OpenApiConfig: Swagger title and version)
├── controller/                 REST controllers: HTTP in and out, no business logic
├── service/                    Business rules (order total, daily limit, cancellation, report)
├── repository/                 Spring Data JPA repositories
├── model/                      JPA entities mapped to database tables
├── contract/
│   ├── request/                Request bodies (records with validation annotations)
│   └── response/               Response bodies (records); entities are never returned directly
├── constant/                   Enums such as order status and nakshatra
├── validation/                 Custom validators, added only when built-in annotations are not enough
└── exception/                  ApiException and subclasses, ErrorResponse, GlobalExceptionHandler

src/main/resources/
├── application.properties
└── db/migration/               Flyway migrations (V1__*.sql, V2__*.sql, ...)

src/test/java/com/tdb/vazhipadu/  Tests, in the same package as the class they test
```

A request flows **controller → service → repository**. The controller validates the request record, the service applies the business rules and maps entities to response records, and any error is thrown as an exception that `GlobalExceptionHandler` turns into the JSON error format.

Folders appear as the PRs add code to them.

Other files in the repository root:

| File | Purpose |
|---|---|
| `pom.xml` | Dependencies and build plugins |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Maven wrapper |
| `.github/pull_request_template.md` | PR description template |

## Endpoints

A sample temple with id `1` is created by the first migration, so you can call the endpoints straight away.

| Method | Path | Description | Success | Errors |
|---|---|---|---|---|
| POST | `/api/temples/{templeId}/offerings` | Add an offering (name, price, active) | 201 | 400, 404, 409 |
| GET | `/api/temples/{templeId}/offerings` | List a temple's offerings, including inactive ones | 200 | 404 |
| PUT | `/api/temples/{templeId}/offerings/{offeringId}` | Update an offering | 200 | 400, 404, 409 |
| POST | `/api/temples/{templeId}/orders` | Place an order; the server calculates the total | 201 | 400, 404, 422 |
| GET | `/api/orders/{orderId}` | Get an order by id | 200 | 404 |

Each PR adds its endpoints to this table and to Swagger.

### Example: place an order

```bash
curl -X POST http://localhost:8080/api/temples/1/offerings   -H "Content-Type: application/json"   -d '{"name":"Pushpanjali","price":30.00,"active":true}'

curl -X POST http://localhost:8080/api/temples/1/orders   -H "Content-Type: application/json"   -d '{"devoteeName":"Lakshmi","phone":"9876543210","nakshatra":"ROHINI",
       "bookingDate":"2026-12-01","items":[{"offeringId":1,"quantity":2}]}'
```

### Business rules

| Rule | Behaviour |
|---|---|
| Order total | Calculated on the server as the sum of price × quantity; any total sent by the client is ignored |
| Price on the order | Each order line keeps the price at the time of booking, so later price changes don't alter old orders |
| Inactive offering | Order rejected with 422 |
| Offering from another temple, or unknown | 404 |
| Same offering twice in one order | 400; send one line with a higher quantity instead |
| Booking date | Today or later, in Indian Standard Time; a past date returns 400 |
| Phone | 10-digit Indian mobile number starting with 6–9 |
| Nakshatra | One of the 27 birth stars, for example `ASHWATHI`, `ROHINI`, `REVATHI` (see `constant/Nakshatra.java`) |
| Offering name | Unique per temple, ignoring case; a duplicate returns 409 |
| Limits | Price up to 8 digits with 2 decimals; quantity 1–100; up to 20 lines per order |

## Error format

All errors, including validation errors, unknown URLs and server errors, return the same JSON body:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/example",
  "timestamp": "2026-10-26T05:00:00Z",
  "fieldErrors": {
    "name": "must not be blank"
  }
}
```

| Field | Meaning |
|---|---|
| `status` | HTTP status code |
| `error` | Standard reason phrase for the status |
| `message` | Human-readable explanation |
| `path` | Request path |
| `timestamp` | When the error happened, in UTC (ISO-8601) |
| `fieldErrors` | Only on validation errors: invalid field → reason |

`GlobalExceptionHandler` maps errors to statuses as follows:

| Cause | Status |
|---|---|
| Bean validation failure on a body or parameter | 400, with `fieldErrors` |
| Malformed or missing JSON body | 400 |
| Wrong parameter type (for example `abc` for a number) | 400 |
| Missing required query parameter | 400 |
| `BadRequestException`, `IllegalArgumentException` | 400 |
| `NotFoundException`, unknown URL | 404 |
| Unsupported HTTP method | 405 |
| `ConflictException` | 409 |
| Unsupported `Content-Type` | 415 |
| Any other exception | 500, message `Unexpected error` with no internal details; the stack trace is logged |

Services signal business errors by throwing a subclass of `ApiException`: `NotFoundException.of("Order", id)`, `ConflictException` or `BadRequestException`. Controllers never build error responses themselves.

## Database and migrations

- The schema is managed **only** by Flyway. Hibernate never creates or changes tables (`ddl-auto=validate`).
- Migrations live in `src/main/resources/db/migration` and are named `V<n>__<description>.sql`, for example `V1__create_offerings_and_orders.sql`.
- Never edit a migration that has already been merged. Add a new one instead.
- Add indexes in migrations for the columns that searches filter and sort on.
- Timestamps are stored in UTC.

## Contributing

`main` is protected. Every change goes through a branch and a pull request:

1. Start a branch from the latest `main`:
   ```bash
   git checkout main && git pull
   git checkout -b feature/p1-offerings
   ```
2. Commit in small, meaningful steps. Don't use messages like `fix`, `changes`, `wip` or `final`.
3. Make sure `./mvnw clean verify` passes.
4. Push and open a PR into `main`. Fill in every section of the template: **Problem**, **Assumptions**, **Decisions & trade-offs** and **How to test**.
5. Review the full diff before merging. Leave at least one comment on a specific line, then address it with a follow-up commit or a reply explaining why no change is needed.
6. Merge, then pull `main` before starting the next branch.

## Pull requests

| Problem | Branch | PR |
|---|---|---|
| P1 – Offerings and Orders API | `feature/p1-offerings` | _pending_ |
| P2 – Order Search | `feature/p2-order-search` | _pending_ |
| P3 – Daily Limits | `feature/p3-daily-limits` | _pending_ |
| P4 – Cancellation and Daily Collection | `feature/p4-cancellation-report` | _pending_ |
