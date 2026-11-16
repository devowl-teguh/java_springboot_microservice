/** Unique-enough demo invoice: INV-YYYYMMDD-XXXX (date in UTC, 4 random base-36 chars). */
export function newInvoice(now: Date = new Date(), random: () => number = Math.random): string {
  const d = now.toISOString().slice(0, 10).replaceAll("-", "");
  const suffix = Array.from({ length: 4 }, () => "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"[Math.floor(random() * 36)]).join("");
  return `INV-${d}-${suffix}`;
}
