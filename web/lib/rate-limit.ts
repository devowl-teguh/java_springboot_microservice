/**
 * Fixed-window limiter kept in memory. Good enough to stop casual abuse of a public demo; each serverless
 * instance has its own window, so the real ceiling is a little higher than `max`.
 */
export function createRateLimiter(max: number, windowMs: number, now: () => number = Date.now) {
  const hits = new Map<string, { start: number; count: number }>();
  return function allow(key: string): boolean {
    const t = now();
    const entry = hits.get(key);
    if (!entry || t - entry.start >= windowMs) {
      hits.set(key, { start: t, count: 1 });
      if (hits.size > 10_000) hits.clear();
      return true;
    }
    entry.count += 1;
    return entry.count <= max;
  };
}
