import { UUID_RE } from "@/lib/schema";
import { forward, problem } from "@/lib/upstream";

/** GET /api/transactions/{id} -> GET /transactions/{id}. */
export async function GET(_request: Request, ctx: RouteContext<"/api/transactions/[id]">) {
  const { id } = await ctx.params;
  if (!UUID_RE.test(id)) return problem(400, "That isn't a valid transaction ID");
  return forward(`/transactions/${id}`);
}
