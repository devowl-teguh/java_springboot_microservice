import { describe, expect, it } from "vitest";
import { createSchema, fieldErrors, UUID_RE } from "@/lib/schema";

const valid = { invoice: "INV-1", amount: 12500, phoneNumber: "08123456789" };

describe("createSchema", () => {
  it("accepts a valid request and trims text", () => {
    const r = createSchema.parse({ ...valid, invoice: "  INV-1  " });
    expect(r.invoice).toBe("INV-1");
  });

  it.each([0.07, 1.1, 1.99, 999999999.99])("accepts %s (at most 2 decimals)", (amount) => {
    expect(createSchema.safeParse({ ...valid, amount }).success).toBe(true);
  });

  it.each([0, -1, 0.001, 1.005, 1_000_000_001])("rejects amount %s", (amount) => {
    expect(createSchema.safeParse({ ...valid, amount }).success).toBe(false);
  });

  it("maps every invalid field, like the API's errors map", () => {
    const r = createSchema.safeParse({ invoice: "", amount: -1, phoneNumber: "" });
    expect(r.success).toBe(false);
    if (!r.success) expect(Object.keys(fieldErrors(r.error)).sort()).toEqual(["amount", "invoice", "phoneNumber"]);
  });

  it("rejects unknown fields and over-long values", () => {
    expect(createSchema.safeParse({ ...valid, admin: true }).success).toBe(false);
    expect(createSchema.safeParse({ ...valid, invoice: "x".repeat(101) }).success).toBe(false);
    expect(createSchema.safeParse({ ...valid, phoneNumber: "1".repeat(31) }).success).toBe(false);
  });
});

describe("UUID_RE", () => {
  it("matches only UUIDs", () => {
    expect(UUID_RE.test("8588405f-f6e6-4eeb-8b4d-9623b5ac897b")).toBe(true);
    expect(UUID_RE.test("not-a-uuid")).toBe(false);
    expect(UUID_RE.test("8588405f-f6e6-4eeb-8b4d-9623b5ac897b/../x")).toBe(false);
  });
});
