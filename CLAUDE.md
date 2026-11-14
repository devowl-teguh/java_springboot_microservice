# CLAUDE.md

Transaction API: Spring Boot 3.4.5 / Java 21 / Maven. Records transactions over REST, publishes them via a
transactional outbox to Kafka, and a consumer marks them `SUCCESS`. Single module, package `com.example.transactions`.
The project skill `.agents/skills/spring-boot-microservices/SKILL.md` holds the coding conventions — follow it.

## Build and test

JDK 21 is Homebrew `openjdk@21` (keg-only) on an Apple Silicon Mac; it may not be on PATH, so set it per command:
`export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`, then use `./mvnw -B test`,
`./mvnw spring-boot:run`, etc. Docker fallback without a JDK:

```sh
# unit tests (what CI runs; Surefire skips *IT)
docker run --rm -v "$PWD":/app -v "$HOME/.m2":/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn -B test

# integration test (Testcontainers: postgres:16-alpine + apache/kafka:3.8.0, ~2 min)
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -v "$PWD":/app -v "$HOME/.m2":/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn -B test -Dtest=TransactionFlowIT
```

CI (`.github/workflows/ci.yml`) runs only unit tests, then packages the jar on pushes to `main`. `TransactionFlowIT`
is not run in CI (no Failsafe plugin). The IT logs a duplicate-key error and shutdown errors when it passes; those are expected.

Local stack: `compose.yaml` runs Kafka (KRaft, `localhost:9092`; containers use `kafka:19092`) plus `kafka-init`, which creates
`transactions-v1` and `.DLT`. Profile `local-db` adds Postgres on `localhost:5433` (db, user and password are all `transactions`); profile `app`
builds the `Dockerfile` and runs the app on 8080. Config comes from `.env` (gitignored, template `.env.example`), which Spring
loads via `spring.config.import: optional:file:.env[.properties]` and Compose reads too. Inside the app container `localhost` is the container itself,
so `APP_DB_URL` overrides `DB_URL` there. Tests also load `.env`, so keep test assertions independent of it (`OpenApiSpecTest` pins the server URL).

## Layout

| Layer | Files | Notes |
|---|---|---|
| `api` | `TransactionController`, `ApiExceptionHandler`, `dto/CreateRequest`, `dto/TransactionResponse` | `POST /transactions` → 202, `GET /transactions/{id}`. Entities never leave via the API; map through records. |
| `domain` | `Transaction`, `Outbox`, repositories, `TransactionService`, exceptions | Business logic in the service. `status` is a plain `String` (`PENDING`/`SUCCESS`), not an enum. |
| `messaging` | `OutboxPublisher`, `TransactionConsumer`, `KafkaErrorConfiguration` | Publisher is `@Scheduled` (`OUTBOX_DELAY_MS`, default 1000) and locks 100 rows with `FOR UPDATE SKIP LOCKED`. |
| `resources` | `application.yml`, `db/migration/V1__transactions.sql` | Schema `transaction_service`; Hibernate `ddl-auto: validate`, so every entity change needs a Flyway migration (`V2__...`). |

## Invariants to preserve

- `TransactionService.create` writes the transaction and its outbox row in one DB transaction; `saveAndFlush` makes a duplicate
  invoice fail early, and the error is mapped to `DuplicateInvoiceException` → 409.
- Event contract v1: the Kafka key and value are both the transaction UUID string, on topic `${KAFKA_TOPIC:transactions-v1}`.
  A breaking change means a new versioned topic/contract.
- Delivery is at least once, so consumers must stay idempotent.
- Errors: `IllegalArgumentException` (bad UUID) and `TransactionNotFoundException` go straight to `<topic>.DLT` on the same partition;
  everything else retries with exponential backoff from 1s to 30s, capped at about 5 minutes.
- The native query in `OutboxRepository` hard-codes the schema name `transaction_service`.
- Money is `BigDecimal`, `NUMERIC(19,2)`, with a DB check `amount > 0`.
- Every error response is RFC 7807 `application/problem+json`. `ApiExceptionHandler` extends `ResponseEntityExceptionHandler`
  so Spring MVC's built-in errors use that format too. Validation 400s add an `errors` map from field to message and
  never echo rejected values. `TransactionControllerTest` (`@WebMvcTest`) covers this.

## OpenAPI

springdoc-openapi 2.8.8 is pinned because 2.8.9+ targets Boot 3.5. Swagger UI is at `/swagger-ui.html`, the spec at `/v3/api-docs`
(and `.yaml`). `OpenApiConfiguration` sets the API info and a fixed server URL so the output is stable. `docs/openapi.yaml`
is committed for importing into API clients. `OpenApiSpecTest` regenerates it and fails when it differs, so **any API change
means rerunning the tests and committing the updated `docs/openapi.yaml`**. Document error responses with
`@Schema(implementation = ApiProblem.class)`, a docs-only record that matches the flattened ProblemDetail JSON;
raw `ProblemDetail.class` documents a misleading nested `properties` field.

## Env vars

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (required), `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`),
`KAFKA_TOPIC`, `OUTBOX_DELAY_MS`, `OPENAPI_SERVER_URL` (default `http://localhost:8080`),
`API_DOCS_ENABLED` (default `true`; turns spec and Swagger UI off). Never commit credentials; `.env` is gitignored.

## Known gaps / candidate next work

- `process()` sets `SUCCESS` unconditionally; there is no state machine and no `FAILED` status.
- Published outbox rows are never cleaned up.
- No Actuator/health checks, auth, or metrics.
- No Failsafe plugin, so the IT is not run by `mvn verify` or in CI.
- Kafka topics are not declared as `NewTopic` beans; the main topic and the DLT rely on the broker auto-creating them or on manual setup.
