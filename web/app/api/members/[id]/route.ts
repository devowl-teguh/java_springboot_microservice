import { UUID_RE } from "@/lib/schema";
import { forward, problem } from "@/lib/upstream";

/** GET /api/members/{id} -> GET /members/{id}. */
export async function GET(_request: Request, ctx: RouteContext<"/api/members/[id]">) {
  const { id } = await ctx.params;
  if (!UUID_RE.test(id)) return problem(400, "That isn't a valid member ID");
  return forward(`/members/${id}`);
}
