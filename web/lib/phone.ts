/** Mirrors the API's Design 0002 rule (PhoneNumbers.java): Indonesian numbers, stored as 62XXXXXXXXX. */
export const PHONE_HINT = "0812…, +62 812… or 62812…; stored as 62…";

export function normalizePhone(input: string): string | null {
  let s = input.trim().replace(/[\s\-.()]/g, "");
  if (s.startsWith("+")) s = s.slice(1);
  if (s.startsWith("0")) s = "62" + s.slice(1);
  return /^62[1-9][0-9]{6,11}$/.test(s) ? s : null;
}
