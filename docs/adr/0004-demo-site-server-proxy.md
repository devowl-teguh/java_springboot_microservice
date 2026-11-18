# ADR 0004: The demo site calls the API through its own server routes

Status: Accepted

## Context

The Next.js demo site needs to call the Lambda API. Calling it from the browser requires CORS on the Function URL,
exposes the URL, and leaves nowhere to limit abuse of a public link.

## Decision

The browser calls only the site's route handlers (`web/app/api/transactions/*`). They validate input with the same rules
as the API, rate limit creates per IP, retry once on a cold-start timeout, and forward to `API_BASE_URL`.

## Consequences

- No CORS configuration; the API URL stays server-side and can change without a site rebuild.
- An extra network hop, and the first request may hit two cold starts (shown as "Waking up the API…").
- The in-memory rate limit is per serverless instance; a shared store (Redis) would be needed for strict limits.
