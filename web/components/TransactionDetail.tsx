"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import ProblemAlert from "./ProblemAlert";
import StatusBadge from "./StatusBadge";
import Timeline from "./Timeline";
import { ApiError, getTransaction } from "@/lib/client";
import { duration, formatAmount, formatTime } from "@/lib/format";
import type { Problem, Transaction } from "@/lib/types";

const POLL_MS = 3000;
const GIVE_UP_MS = 3 * 60_000;

export default function TransactionDetail({ id }: { id: string }) {
  const [tx, setTx] = useState<Transaction | null>(null);
  const [problem, setProblem] = useState<Problem | null>(null);
  const [stalled, setStalled] = useState(false);
  const [now, setNow] = useState(0);
  const [polls, setPolls] = useState(0);
  const started = useRef(0);

  useEffect(() => {
    let stop = false;
    let timer: ReturnType<typeof setTimeout>;
    started.current = Date.now();
    async function tick() {
      try {
        const next = await getTransaction(id);
        if (stop) return;
        setTx(next);
        setNow(Date.now());
        setProblem(null);
        setPolls((n) => n + 1);
        if (next.status !== "PENDING") return; // SUCCESS and FAILED are terminal
        if (Date.now() - started.current > GIVE_UP_MS) {
          setStalled(true);
          return;
        }
      } catch (e) {
        if (stop) return;
        const p = e instanceof ApiError ? e.problem : { status: 0, detail: "Couldn't reach the server." };
        setProblem(p);
        if (p.status === 404 || p.status === 400) return; // will not fix itself
      }
      timer = setTimeout(tick, POLL_MS);
    }
    tick();
    return () => {
      stop = true;
      clearTimeout(timer);
    };
  }, [id]);

  // Re-render once a second so the elapsed time counts up while PENDING.
  useEffect(() => {
    if (tx?.status !== undefined && tx?.status !== "PENDING") return;
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, [tx?.status]);

  if (!tx) {
    return problem ? (
      <div className="space-y-4">
        <ProblemAlert problem={problem} />
        <Link href="/lookup" className="btn">Look up another transaction</Link>
      </div>
    ) : (
      <p className="text-muted">Loading transaction… the first request can take a few seconds while Lambda starts.</p>
    );
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="font-mono text-xl font-semibold break-all">{tx.invoice}</h1>
        <StatusBadge status={tx.status} />
        <span className="ml-auto text-sm text-faint tabular-nums" title="Time since creation">
          ⏱ {duration(tx.createdAt, tx.completedAt, now)}
        </span>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        {[
          ["Amount", formatAmount(tx.amount)],
          ["Phone", tx.phoneNumber],
          ["Created", formatTime(tx.createdAt)],
        ].map(([label, value]) => (
          <div key={label} className="rounded-lg bg-sunk px-3 py-2.5">
            <p className="text-xs text-muted">{label}</p>
            <p className="font-medium tabular-nums">{value}</p>
          </div>
        ))}
      </div>
      {tx.memberId ? (
        <p className="text-sm text-muted">
          Member:{" "}
          <Link href={`/members/${tx.memberId}`} className="font-mono text-accent underline">
            {tx.phoneNumber}
          </Link>{" "}
          · see all of this member&rsquo;s transactions
        </p>
      ) : (
        <p className="text-xs text-faint">Created before members existed, so it has no member.</p>
      )}

      <section className="card p-5">
        <h2 className="mb-2 text-sm font-medium text-muted">What is happening</h2>
        <Timeline tx={tx} />
        {tx.status === "PENDING" && !stalled && (
          <p className="mt-3 text-xs text-faint">Checking every 3 seconds. The outbox relay runs once a minute, so this takes up to about a minute.</p>
        )}
        {stalled && (
          <p className="mt-3 text-sm text-pending">This is taking longer than usual. Refresh the page to check again.</p>
        )}
        {problem && <div className="mt-3"><ProblemAlert problem={problem} /></div>}
      </section>

      <details className="card p-4 text-sm">
        <summary className="cursor-pointer text-muted">Raw API response</summary>
        <p className="mt-3 font-mono text-xs text-faint">
          GET /transactions/{tx.id} · 200 OK · poll #{polls}
        </p>
        <pre className="mt-2 overflow-x-auto rounded-lg bg-sunk p-3 font-mono text-xs leading-relaxed">{JSON.stringify(tx, null, 2)}</pre>
      </details>

      <div className="flex flex-wrap gap-2">
        <Link href="/" className="btn btn-primary">Create another</Link>
        <Link href="/lookup" className="btn">Recent transactions</Link>
      </div>
    </div>
  );
}
