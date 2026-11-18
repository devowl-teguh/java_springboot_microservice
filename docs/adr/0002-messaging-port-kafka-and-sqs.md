# ADR 0002: Hide the message broker behind a port, with Kafka and SQS adapters

Status: Accepted

## Context

The service is designed around Kafka, but a public demo has to run for free. Amazon MSK costs tens of dollars a month,
while Lambda, SQS and EventBridge stay within the AWS free tier at demo traffic.

## Decision

Introduce `TransactionEventPublisher` (port). `KafkaTransactionEventPublisher` is active by default;
`SqsTransactionEventPublisher` is active with the `aws` Spring profile, where Lambda handlers replace the scheduler and the
Kafka listener. Domain code, the outbox and the API are shared.

## Consequences

- One codebase runs locally on Kafka and in the cloud on Lambda + SQS; tests cover both (`TransactionFlowIT`, `AwsModeIT`).
- AWS mode processes more slowly (EventBridge schedules run at most once a minute) and has cold starts (reduced with SnapStart).
- SQS gives no ordering or replay; acceptable because each event is independent and processing is idempotent.
- Adding another broker (for example Google Pub/Sub) means one adapter plus a profile.
