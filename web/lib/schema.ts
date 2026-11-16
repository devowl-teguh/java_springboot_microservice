import { z } from "zod";

/** Mirrors the Java CreateRequest rules, plus a demo-sized upper bound on amount. */
export const MAX_AMOUNT = 1_000_000_000;

export const createSchema = z
  .object({
    invoice: z.string().trim().min(1, "Enter an invoice number").max(100, "Use at most 100 characters"),
    amount: z
      .number({ error: "Enter an amount" })
      .min(0.01, "Amount must be at least 0.01")
      .max(MAX_AMOUNT, "Use an amount up to 1,000,000,000")
      // n * 100 must be a whole number; the tolerance absorbs float noise such as 0.07 * 100 = 7.000000000000001.
      .refine((n) => Math.abs(n * 100 - Math.round(n * 100)) < 1e-6, "Use at most 2 decimal places"),
    phoneNumber: z.string().trim().min(1, "Enter a phone number").max(30, "Use at most 30 characters"),
  })
  .strict();

export type CreateInput = z.infer<typeof createSchema>;

export const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Field -> first message, the same shape as the API's `errors` map. */
export function fieldErrors(error: z.ZodError): Record<string, string> {
  const out: Record<string, string> = {};
  for (const issue of error.issues) {
    const key = String(issue.path[0] ?? "body");
    out[key] ??= issue.message;
  }
  return out;
}
