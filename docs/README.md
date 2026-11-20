# Documentation

| Document | Purpose |
|---|---|
| [openapi.yaml](openapi.yaml) | API contract, generated from the code and checked in CI (`OpenApiSpecTest`) |
| [design/](design/) | Design docs for changes with an API, data or flow impact; agreed before implementation starts |
| [adr/](adr/) | Architecture decision records: one decision, its context and consequences |

Design docs: [0001 Idempotent creation and a FAILED status](design/0001-idempotency-and-failed-status.md) ·
[0002 Members and normalized phone numbers](design/0002-members.md)

ADRs: [0001 Transactional outbox](adr/0001-transactional-outbox.md) ·
[0002 Messaging port with Kafka and SQS](adr/0002-messaging-port-kafka-and-sqs.md) ·
[0003 Schema per service](adr/0003-schema-per-service.md) ·
[0004 Demo site server proxy](adr/0004-demo-site-server-proxy.md)

New documents start from the templates `design/0000-template.md` and `adr/0000-template.md`.
