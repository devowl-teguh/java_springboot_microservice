# ADR 0001: Publish events with a transactional outbox

Status: Accepted

## Context

Creating a transaction must also publish an event for asynchronous processing. A database write and a broker publish
cannot be committed atomically: publishing first can announce a transaction that never commits, and committing first
can lose the event if the process crashes before publishing.

## Decision

Write the transaction and an `outbox` row in the same database transaction. A separate relay locks pending rows with
`SELECT … FOR UPDATE SKIP LOCKED`, publishes them, and marks them published only after the broker acknowledges.

## Consequences

- No lost events: if the broker is down, rows wait in the outbox and the relay retries.
- Delivery is at least once (a crash between publish and mark causes a resend), so consumers must be idempotent.
- Events are delayed by the relay interval (1 second locally, 1 minute on Lambda).
- Published rows accumulate and need a cleanup policy (open item).
- Alternatives: change data capture (Debezium) avoids polling but needs more infrastructure; dual writes were rejected
  because they can lose or invent events.
