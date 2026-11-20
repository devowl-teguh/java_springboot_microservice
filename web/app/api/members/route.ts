import { forward } from "@/lib/upstream";

/** GET /api/members?limit=N -> GET /members?limit=N (phone numbers masked by the API). */
export async function GET(request: Request) {
  const raw = Number(new URL(request.url).searchParams.get("limit") ?? 20);
  const limit = Number.isInteger(raw) ? Math.min(Math.max(raw, 1), 50) : 20;
  return forward(`/members?limit=${limit}`);
}
