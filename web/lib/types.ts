export type Status = "PENDING" | "SUCCESS";

export interface Transaction {
  id: string;
  invoice: string;
  amount: number;
  phoneNumber: string;
  status: Status;
  createdAt: string;
  publishedAt: string | null;
  completedAt: string | null;
}

/** Item of GET /transactions; the API masks the phone number. */
export type TransactionSummary = Omit<Transaction, "publishedAt" | "completedAt">;

/** RFC 7807 body returned by the Java API (and by our proxy for its own errors). */
export interface Problem {
  status: number;
  title?: string;
  detail?: string;
  errors?: Record<string, string>;
}
