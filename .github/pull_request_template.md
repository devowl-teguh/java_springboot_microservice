## What and why

<!-- One or two sentences. Link the design doc or issue. -->

Design doc / ADR:

## Changes

-

## API contract

- [ ] No API change
- [ ] API changed: `docs/openapi.yaml` regenerated and `web/lib/types.ts` updated; backward compatible for existing clients

## Testing

<!-- What you ran and what it covers. Paste counts, not just "tests pass". -->

- [ ] Unit tests
- [ ] Integration tests (Testcontainers) for persistence or messaging changes
- [ ] Duplicate, concurrent and failure paths covered where relevant
- [ ] Manually verified (local stack or live demo)

## Rollout

- [ ] Flyway migration is additive and safe with the currently deployed version
- [ ] Needs a redeploy of: Lambda / demo site / none

## Reviewer notes

<!-- Anything to look at closely, known trade-offs, follow-ups. -->
