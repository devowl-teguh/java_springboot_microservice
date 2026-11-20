# Transaction demo website

Live: https://web-seven-navy-29.vercel.app

Next.js 16 site that lets anyone try the [Transaction API](../README.md): create a transaction and watch it move from
`PENDING` to `SUCCESS` through the transactional outbox, SQS and the consumer Lambda.

| Page | What it does |
|---|---|
| `/` | Create a transaction (unique invoice pre-filled), plus buttons that trigger a real 409 and a 400 |
| `/transactions/[id]` | Polls every 3 s and shows a timeline built from `createdAt`, `publishedAt` and `completedAt` |
| `/lookup` | Open a transaction by ID, or pick from the 10 most recent (phone numbers masked by the API) |
| `/how-it-works` | Architecture and reliability notes |

The browser only talks to this site. Route handlers in `app/api/transactions` proxy to the Java API, so its URL stays
server-side, requests are validated with the same rules (Zod, `lib/schema.ts`), creates are rate limited to 10 per
minute per IP, and a Lambda cold start gets one automatic retry.

## Run locally

```sh
cp .env.example .env.local   # API_BASE_URL points at the live Lambda Function URL
npm install
npm run dev                  # http://localhost:3000
```

Checks (also run in GitHub Actions when `web/**` changes): `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`.

## Deploy (Vercel)

Import the GitHub repository in Vercel, set **Root Directory** to `web`, add the environment variable `API_BASE_URL`, and
deploy. Pushes to `main` redeploy automatically; pull requests get preview URLs.
