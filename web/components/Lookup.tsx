"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import ProblemAlert from "./ProblemAlert";
import StatusBadge from "./StatusBadge";
import { ApiError, listRecent } from "@/lib/client";
import { formatAmount, formatTime } from "@/lib/format";
import { UUID_RE } from "@/lib/schema";
import type { Problem, TransactionSummary } from "@/lib/types";

export default function Lookup() {
  const router = useRouter();
  const [id, setId] = useState("");
  const [idError, setIdError] = useState("");
  const [recent, setRecent] = useState<TransactionSummary[] | null>(null);
  const [problem, setProblem] = useState<Problem | null>(null);

  useEffect(() => {
    listRecent(10)
      .then(setRecent)
      .catch((e) => setProblem(e instanceof ApiError ? e.problem : { status: 0, detail: "Couldn't load recent transactions." }));
  }, []);

  return (
    <div className="space-y-6">
      <form
        className="card flex flex-col gap-2 p-5 sm:flex-row sm:items-start"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          const value = id.trim();
          if (!UUID_RE.test(value)) {
            setIdError("Enter a transaction ID like 3fa85f64-5717-4562-b3fc-2c963f66afa6");
            return;
          }
          router.push(`/transactions/${value}`);
        }}
      >
        <div className="flex-1">
          <label htmlFor="tx-id" className="sr-only">Transaction ID</label>
          <input
            id="tx-id"
            className="field font-mono"
            placeholder="3fa85f64-5717-4562-b3fc-2c963f66afa6"
            value={id}
            onChange={(e) => {
              setId(e.target.value);
              setIdError("");
            }}
          />
          {idError && <p className="mt-1 text-xs text-danger">{idError}</p>}
        </div>
        <button type="submit" className="btn btn-primary">Open</button>
      </form>

      <section className="space-y-2">
        <h2 className="text-sm font-medium text-muted">Recent transactions</h2>
        {problem && <ProblemAlert problem={problem} />}
        {!recent && !problem && <p className="text-sm text-muted">Loading…</p>}
        {recent?.length === 0 && (
          <p className="text-sm text-muted">
            No transactions yet. <Link href="/" className="text-accent underline">Create the first one</Link>.
          </p>
        )}
        {recent && recent.length > 0 && (
          <ul className="card divide-y divide-line">
            {recent.map((t) => (
              <li key={t.id}>
                <Link href={`/transactions/${t.id}`} className="flex flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3 text-sm hover:bg-sunk">
                  <span className="min-w-0 flex-1 truncate font-mono">{t.invoice}</span>
                  <span className="tabular-nums text-muted">{formatAmount(t.amount)}</span>
                  <span className="font-mono text-xs text-faint">{t.phoneNumber}</span>
                  <span className="text-xs text-faint tabular-nums">{formatTime(t.createdAt)}</span>
                  <StatusBadge status={t.status} />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
