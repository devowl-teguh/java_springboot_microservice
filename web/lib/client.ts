import type { MemberDetail, MemberSummary, Problem, Transaction, TransactionSummary } from "./types";

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

export interface CreateOutcome {
  transaction: Transaction;
  /** True when the API recognised the Idempotency-Key and returned the existing transaction. */
  replayed: boolean;
}

export async function createTransaction(
  input: { invoice: string; amount: number; phoneNumber: string },
  idempotencyKey?: string,
): Promise<CreateOutcome> {
  const res = await fetch("/api/transactions", {
    method: "POST",
    headers: { "content-type": "application/json", ...(idempotencyKey ? { "idempotency-key": idempotencyKey } : {}) },
    body: JSON.stringify(input),
  });
  const replayed = res.headers.get("idempotent-replayed") === "true";
  return { transaction: await parse<Transaction>(res), replayed };
}

export async function getTransaction(id: string): Promise<Transaction> {
  return parse(await fetch(`/api/transactions/${encodeURIComponent(id)}`, { cache: "no-store" }));
}

export async function listRecent(limit = 10): Promise<TransactionSummary[]> {
  return parse(await fetch(`/api/transactions?limit=${limit}`, { cache: "no-store" }));
}

export async function listMembers(limit = 20): Promise<MemberSummary[]> {
  return parse(await fetch(`/api/members?limit=${limit}`, { cache: "no-store" }));
}

export async function getMember(id: string): Promise<MemberDetail> {
  return parse(await fetch(`/api/members/${encodeURIComponent(id)}`, { cache: "no-store" }));
}
