import type { Problem } from "./types";

/** Server-only: forwards a request to the Java API on Lambda. The base URL never reaches the browser. */
const TIMEOUT_MS = 10_000;
/** Response headers from the API that the browser may need. */
const PASS_THROUGH = ["idempotent-replayed"];

export function apiBaseUrl(): string {
  const url = process.env.API_BASE_URL;
  if (!url) throw new Error("API_BASE_URL is not set");
  return url.replace(/\/+$/, "");
}

export function problem(status: number, detail: string, errors?: Record<string, string>): Response {
  const body: Problem = { status, title: statusTitle(status), detail, ...(errors ? { errors } : {}) };
  return Response.json(body, { status, headers: { "content-type": "application/problem+json" } });
}

function statusTitle(status: number): string {
  return ({ 400: "Bad Request", 404: "Not Found", 409: "Conflict", 429: "Too Many Requests", 502: "Bad Gateway", 504: "Gateway Timeout" } as Record<number, string>)[status] ?? "Error";
}

/**
 * Calls the API and passes its status and body through unchanged (including RFC 7807 errors).
 * Retries once on timeout or network error, which covers a Lambda cold start.
 */
export async function forward(path: string, init: RequestInit = {}, fetchImpl: typeof fetch = fetch): Promise<Response> {
  const url = apiBaseUrl() + path;
  for (let attempt = 1; ; attempt++) {
    try {
      const res = await fetchImpl(url, { ...init, cache: "no-store", signal: AbortSignal.timeout(TIMEOUT_MS) });
      const body = await res.text();
      const headers = new Headers({ "content-type": res.headers.get("content-type") ?? "application/json", "cache-control": "no-store" });
      for (const name of PASS_THROUGH) {
        const value = res.headers.get(name);
        if (value) headers.set(name, value);
      }
      return new Response(body, { status: res.status, headers });
    } catch (e) {
      if (attempt >= 2) {
        const timedOut = e instanceof DOMException && e.name === "TimeoutError";
        return timedOut
          ? problem(504, "The API took too long to respond. Try again in a few seconds.")
          : problem(502, "Couldn't reach the API. Try again in a few seconds.");
      }
    }
  }
}
