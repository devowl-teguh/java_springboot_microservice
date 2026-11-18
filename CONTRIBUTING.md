# Contributing

## Workflow

1. **Design first for anything with an API, data or flow impact.** Write a design doc from
   [`docs/design/0000-template.md`](docs/design/0000-template.md): scope and non-goals, the API contract, end-to-end
   sequence diagrams (including failure paths) and a test plan. Review it with backend, frontend and QA before the work
   is planned. Record lasting architecture choices as an ADR in [`docs/adr/`](docs/adr/).
2. **Branch from `main`:** `feat/…`, `fix/…`, `docs/…`, `chore/…`.
3. **Open a pull request** using the template. Keep PRs focused; split refactors from behaviour changes.
4. **Review:** at least one approval. Reviewers check the contract, tests and failure handling, not only style.
5. **Merge** with squash or rebase once CI is green. `main` is protected: no direct pushes, CI must pass.

## Definition of done

A change is done when:

- [ ] Behaviour matches the agreed design doc (or the PR explains the deviation).
- [ ] The API contract is updated: `docs/openapi.yaml` regenerated (`OpenApiSpecTest`) and `web/lib/types.ts` matched.
- [ ] Tests cover the happy path, validation, duplicates and concurrency where relevant, and failure or retry paths.
- [ ] Database changes are a new Flyway migration that is backward compatible with the running version.
- [ ] Errors are RFC 7807 ProblemDetail; no stack traces or personal data in responses or logs.
- [ ] CI is green: Java unit tests, and the Web workflow when `web/` changed. Integration tests run locally for
      changes to persistence or messaging (`TransactionFlowIT`, `AwsModeIT`, `ApiLambdaHandlerIT`).
- [ ] Docs updated: README, `CLAUDE.md` invariants, and a design doc or ADR when a decision changed.
- [ ] Deployed and smoke-tested on the live demo when the change affects it.

## Code review checklist

- Is the change idempotent under at-least-once delivery and client retries?
- Are database writes that must be atomic in one transaction, and is anything published outside the outbox?
- Are new failure modes handled explicitly (retry, dead letter, terminal status), not swallowed?
- Are inputs validated and limits enforced at the boundary?
- Is the change observable (logs with the transaction ID, no secrets)?
- Would a new team member understand it from the code, tests and docs?

## Commands

See [`CLAUDE.md`](CLAUDE.md) for build, test and local stack commands, and [`web/README.md`](web/README.md) for the demo site.
