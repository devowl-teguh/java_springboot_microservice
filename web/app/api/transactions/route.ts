import { createSchema, fieldErrors } from "@/lib/schema";
import { createRateLimiter } from "@/lib/rate-limit";
import { forward, problem } from "@/lib/upstream";

const allowCreate = createRateLimiter(10, 60_000);

function clientIp(request: Request): string {
  return request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || request.headers.get("x-real-ip") || "unknown";
}

/** POST /api/transactions -> POST /transactions (validated and rate limited first). */
export async function POST(request: Request) {
  if (!allowCreate(clientIp(request))) {
    return problem(429, "Too many transactions from your network. Wait a minute and try again.");
  }
  let json: unknown;
  try {
    json = await request.json();
  } catch {
    return problem(400, "Request body must be JSON");
  }
  const parsed = createSchema.safeParse(json);
  if (!parsed.success) return problem(400, "Request validation failed", fieldErrors(parsed.error));

  return forward("/transactions", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(parsed.data),
  });
}

/** GET /api/transactions?limit=N -> GET /transactions?limit=N (recent list, phones masked by the API). */
export async function GET(request: Request) {
  const raw = Number(new URL(request.url).searchParams.get("limit") ?? 10);
  const limit = Number.isInteger(raw) ? Math.min(Math.max(raw, 1), 50) : 10;
  return forward(`/transactions?limit=${limit}`);
}
