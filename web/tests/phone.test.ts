import { describe, expect, it } from "vitest";
import { normalizePhone } from "@/lib/phone";
import { createSchema } from "@/lib/schema";

// Same cases as PhoneNumbersTest.java, so the site and the API agree.
describe("normalizePhone", () => {
  it.each([
    ["08123456789", "628123456789"],
    ["+62 812-3456-789", "628123456789"],
    ["628123456789", "628123456789"],
    ["6285544632", "6285544632"],
    ["(0812) 3456.789", "628123456789"],
    ["+62812345678901", "62812345678901"],
  ])("%s -> %s", (input, expected) => expect(normalizePhone(input)).toBe(expected));

  it.each(["+1 415 555 0100", "12345", "62 0812 3456 789", "62812345", "6281234567890123456", "0812abc456", "++628123456789", "", "   "])(
    "rejects %j",
    (input) => expect(normalizePhone(input)).toBeNull(),
  );

  it("is enforced by the create schema", () => {
    expect(createSchema.safeParse({ invoice: "INV-1", amount: 1, phoneNumber: "+1 415 555 0100" }).success).toBe(false);
    expect(createSchema.safeParse({ invoice: "INV-1", amount: 1, phoneNumber: "0812-3456-789" }).success).toBe(true);
  });
});
