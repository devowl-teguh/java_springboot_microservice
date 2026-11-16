import type { Problem, Transaction, TransactionSummary } from "./types";

/** Browser-side calls to our own /api routes. */
export class ApiError extends Error {
  constructor(public readonly problem: Problem) {
    super(problem.detail ?? `Request failed (${problem.status})`);
  }
}

async function parse<T>(res: Response): Promise<T> {
  const text = await res.text();
  const body = text ? JSON.parse(text) : null;
  if (!res.ok) throw new ApiError({ status: res.status, ...(body ?? {}) });
  return body as T;
}

export async function createTransaction(input: { invoice: string; amount: number; phoneNumber: string }): Promise<Transaction> {
  return parse(await fetch("/api/transactions", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(input) }));
}

export async function getTransaction(id: string): Promise<Transaction> {
  return parse(await fetch(`/api/transactions/${encodeURIComponent(id)}`, { cache: "no-store" }));
}

export async function listRecent(limit = 10): Promise<TransactionSummary[]> {
  return parse(await fetch(`/api/transactions?limit=${limit}`, { cache: "no-store" }));
}
