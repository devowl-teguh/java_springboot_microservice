"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import ProblemAlert from "./ProblemAlert";
import { ApiError, listMembers } from "@/lib/client";
import { formatTime } from "@/lib/format";
import type { MemberSummary, Problem } from "@/lib/types";

export default function MemberList() {
  const [members, setMembers] = useState<MemberSummary[] | null>(null);
  const [problem, setProblem] = useState<Problem | null>(null);

  useEffect(() => {
    listMembers(20)
      .then(setMembers)
      .catch((e) => setProblem(e instanceof ApiError ? e.problem : { status: 0, detail: "Couldn't load members." }));
  }, []);

  if (problem) return <ProblemAlert problem={problem} />;
  if (!members) return <p className="text-sm text-muted">Loading…</p>;
  if (members.length === 0) {
    return (
      <p className="text-sm text-muted">
        No members yet. <Link href="/" className="text-accent underline">Create a transaction</Link> to add the first one.
      </p>
    );
  }
  return (
    <ul className="card divide-y divide-line">
      {members.map((m) => (
        <li key={m.id}>
          <Link href={`/members/${m.id}`} className="flex flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3 text-sm hover:bg-sunk">
            <span className="min-w-0 flex-1 font-mono">{m.phoneNumber}</span>
            <span className="text-xs text-faint tabular-nums">Joined {formatTime(m.createdAt)}</span>
            <span aria-hidden="true" className="text-faint">→</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
