export function formatAmount(amount: number): string {
  return new Intl.NumberFormat("id-ID", { style: "currency", currency: "IDR", minimumFractionDigits: 2 }).format(amount);
}

export function formatTime(iso: string | null): string {
  if (!iso) return "-";
  return new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit", second: "2-digit" }).format(new Date(iso));
}

/** "4.2s" or "1m 05s" between two ISO timestamps. */
export function duration(fromIso: string, toIso: string | null, nowMs: number = Date.now()): string {
  const ms = Math.max(0, (toIso ? Date.parse(toIso) : nowMs) - Date.parse(fromIso));
  const s = ms / 1000;
  if (s < 60) return `${s.toFixed(1)}s`;
  const m = Math.floor(s / 60);
  return `${m}m ${String(Math.floor(s % 60)).padStart(2, "0")}s`;
}
