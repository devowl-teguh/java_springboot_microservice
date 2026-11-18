# ADR 0003: Own a dedicated Postgres schema per service

Status: Accepted

## Context

Microservices should own their data, but separate database servers per service are costly for a small system and the
free Neon plan offers one database.

## Decision

All tables live in schema `transaction_service`, created and migrated by Flyway; Hibernate uses it as the default schema.

## Consequences

- Other services can share the database without name clashes, and ownership is visible in every table name.
- Access can be limited per schema with database roles.
- Services still share CPU and connections; moving to a database per service later is a configuration change.
- Cross-schema joins are possible but are against the rule: services integrate through APIs and events.
