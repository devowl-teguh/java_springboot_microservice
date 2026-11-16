import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { forward } from "@/lib/upstream";

describe("forward", () => {
  beforeEach(() => vi.stubEnv("API_BASE_URL", "https://api.example/"));
  afterEach(() => vi.unstubAllEnvs());

  it("passes the API's status and RFC 7807 body through unchanged", async () => {
    const body = JSON.stringify({ status: 409, detail: "Invoice already exists" });
    const fetchImpl = vi.fn(async () => new Response(body, { status: 409, headers: { "content-type": "application/problem+json" } }));
    const res = await forward("/transactions", { method: "POST" }, fetchImpl as unknown as typeof fetch);
    expect(fetchImpl).toHaveBeenCalledWith("https://api.example/transactions", expect.objectContaining({ method: "POST" }));
    expect(res.status).toBe(409);
    expect(res.headers.get("content-type")).toBe("application/problem+json");
    expect(await res.text()).toBe(body);
  });

  it("retries once on a network error (cold start), then succeeds", async () => {
    const fetchImpl = vi.fn()
      .mockRejectedValueOnce(new TypeError("fetch failed"))
      .mockResolvedValueOnce(new Response("[]", { status: 200 }));
    const res = await forward("/transactions", {}, fetchImpl as unknown as typeof fetch);
    expect(fetchImpl).toHaveBeenCalledTimes(2);
    expect(res.status).toBe(200);
  });

  it("returns 502 problem JSON after two failures", async () => {
    const fetchImpl = vi.fn().mockRejectedValue(new TypeError("fetch failed"));
    const res = await forward("/transactions", {}, fetchImpl as unknown as typeof fetch);
    expect(res.status).toBe(502);
    expect((await res.json()).detail).toMatch(/Couldn't reach the API/);
  });
});
