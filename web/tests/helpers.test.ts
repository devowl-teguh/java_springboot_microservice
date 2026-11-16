import { describe, expect, it } from "vitest";
import { newInvoice } from "@/lib/invoice";
import { createRateLimiter } from "@/lib/rate-limit";
import { duration } from "@/lib/format";
import { stages } from "@/components/Timeline";
import type { Transaction } from "@/lib/types";

describe("newInvoice", () => {
  it("formats INV-YYYYMMDD-XXXX in UTC", () => {
    expect(newInvoice(new Date("2026-11-16T23:59:00Z"), () => 0)).toBe("INV-20261116-0000");
    expect(newInvoice()).toMatch(/^INV-\d{8}-[0-9A-Z]{4}$/);
  });
});

describe("createRateLimiter", () => {
  it("allows max requests per window per key, then resets", () => {
    let t = 0;
    const allow = createRateLimiter(2, 1000, () => t);
    expect([allow("a"), allow("a"), allow("a")]).toEqual([true, true, false]);
    expect(allow("b")).toBe(true);
    t = 1000;
    expect(allow("a")).toBe(true);
  });
});

describe("duration", () => {
  it("formats seconds and minutes", () => {
    expect(duration("2026-01-01T00:00:00Z", "2026-01-01T00:00:04.2Z")).toBe("4.2s");
    expect(duration("2026-01-01T00:00:00Z", "2026-01-01T00:01:05Z")).toBe("1m 05s");
  });
});

describe("timeline stages", () => {
  const base: Transaction = {
    id: "x", invoice: "INV-1", amount: 1, phoneNumber: "0812", status: "PENDING",
    createdAt: "2026-01-01T00:00:00Z", publishedAt: null, completedAt: null,
  };
  const states = (tx: Transaction) => stages(tx).map((s) => s.state);

  it("follows the outbox flow", () => {
    expect(states(base)).toEqual(["done", "now", "todo", "todo"]);
    expect(states({ ...base, publishedAt: "2026-01-01T00:00:40Z" })).toEqual(["done", "done", "now", "todo"]);
    expect(states({ ...base, status: "SUCCESS", publishedAt: "2026-01-01T00:00:40Z", completedAt: "2026-01-01T00:00:41Z" }))
      .toEqual(["done", "done", "done", "done"]);
  });

  it("treats SUCCESS without timestamps (older rows) as fully done", () => {
    expect(states({ ...base, status: "SUCCESS" })).toEqual(["done", "done", "done", "done"]);
  });
});
