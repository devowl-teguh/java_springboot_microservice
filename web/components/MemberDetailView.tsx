"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import ProblemAlert from "./ProblemAlert";
import StatusBadge from "./StatusBadge";
import { ApiError, getMember } from "@/lib/client";
import { formatAmount, formatTime } from "@/lib/format";
import type { MemberDetail, Problem } from "@/lib/types";

export default function MemberDetailView({ id }: { id: string }) {
  const [member, setMember] = useState<MemberDetail | null>(null);
  const [problem, setProblem] = useState<Problem | null>(null);

  useEffect(() => {
    getMember(id)
      .then(setMember)
      .catch((e) => setProblem(e instanceof ApiError ? e.problem : { status: 0, detail: "Couldn't load the member." }));
  }, [id]);

  if (problem) {
    return (
      <div className="space-y-4">
        <ProblemAlert problem={problem} />
        <Link href="/members" className="btn">All members</Link>
      </div>
    );
  }
  if (!member) return <p className="text-muted">Loading member… the first request can take a few seconds while Lambda starts.</p>;

  return (
    <div className="space-y-5">
      <div className="space-y-1">
        <p className="text-sm text-muted">Member</p>
        <h1 className="font-mono text-xl font-semibold">{member.phoneNumber}</h1>
        <p className="text-sm text-faint">Joined {formatTime(member.createdAt)} · {member.transactions.length} recent transaction{member.transactions.length === 1 ? "" : "s"}</p>
      </div>
      <section className="space-y-2">
        <h2 className="text-sm font-medium text-muted">Transactions</h2>
        <ul className="card divide-y divide-line">
          {member.transactions.map((t) => (
            <li key={t.id}>
              <Link href={`/transactions/${t.id}`} className="flex flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3 text-sm hover:bg-sunk">
                <span className="min-w-0 flex-1 truncate font-mono">{t.invoice}</span>
                <span className="tabular-nums text-muted">{formatAmount(t.amount)}</span>
                <span className="text-xs text-faint tabular-nums">{formatTime(t.createdAt)}</span>
                <StatusBadge status={t.status} />
              </Link>
            </li>
          ))}
        </ul>
      </section>
      <div className="flex flex-wrap gap-2">
        <Link href="/" className="btn btn-primary">Create a transaction</Link>
        <Link href="/members" className="btn">All members</Link>
      </div>
    </div>
  );
}
