---
name: spring-boot-microservices
description: Build or change Java Spring Boot REST APIs and Kafka-based microservices using simple layered architecture, PostgreSQL, and reliable event handling.
---

# Spring Boot Microservices

## Workflow

Understand requirements and inspect existing project conventions before changing code. Prefer the existing compatible versions and build tooling; for a new project, use Java 21, a compatible Spring Boot version, Maven, PostgreSQL, Spring Data JPA, Spring Kafka, Flyway, JUnit 5, Mockito, and Testcontainers. Keep changes small and reviewable, explain important decisions, and avoid unnecessary dependencies. Run relevant tests and report what actually ran and any remaining risks.

When the user requests step-by-step learning, explain the next step, show only the code needed for it, let the user write and review it, and wait for their request before continuing. Otherwise, complete the requested work without imposing pauses.

## Architecture and API

- Use a simple controller → service → repository structure. Add DTOs, mappers, event producers and consumers, configuration, and centralized exception handling as needed. Avoid unnecessary abstractions, CQRS, event sourcing, or deployment complexity.
- Keep controllers thin, put business logic in services, validate request DTOs with Jakarta Validation, use appropriate HTTP status codes and consistent error responses, and paginate large collections. Do not expose persistence entities directly.
- Keep service boundaries business-oriented. Each service owns its data; do not directly change another service's tables. Use events for asynchronous communication and HTTP when an immediate response is needed; configure timeouts and avoid circular dependencies.

## Data and transaction consistency

- Use Flyway migrations, database transactions for atomic database operations, and constraints to protect integrity. Check for inefficient queries such as N+1. Use BigDecimal rather than floating-point types for money.
- Do not assume a database write and Kafka publish are atomic together. When reliable publication is required, write the business change and an outbox record in the same database transaction; publish separately with retry and account for duplicate delivery.
- Use stable identifiers and enforce idempotency for duplicate transaction requests and event processing, including concurrent attempts. Define failure recovery and document eventual consistency between services.

## Kafka

- Define explicit, versioned event contracts and stable message keys. Preserve ordering where the business requires it and account for Kafka's partition scope.
- Make consumers idempotent; do not assume exactly-once business processing. Retry transient failures with bounded backoff, handle permanent failures explicitly, and use dead-letter topics when appropriate. Never silently swallow processing failures or acknowledge an event before its required durable work succeeds.

## Security and observability

- Keep secrets out of source code; use environment variables or the project's secret management. Validate untrusted input, apply authentication and authorization as required, and avoid exposing stack traces or logging passwords, tokens, or personal data.
- Use structured logging and appropriate health/operational visibility, such as Spring Boot Actuator, without exposing sensitive information. Document APIs with OpenAPI when appropriate.

## Testing

- Test business logic, API validation and error handling, and persistence behavior. Use integration tests with Testcontainers where appropriate for PostgreSQL and Kafka.
- Cover duplicate requests and event delivery, concurrent idempotency where relevant, transient failures and retry exhaustion, and outbox recovery when implemented. Verify the observable behavior and transaction boundaries rather than only mocking the implementation.
