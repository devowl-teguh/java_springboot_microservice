"use client";

import { useRouter } from "next/navigation";
import { useState, useSyncExternalStore } from "react";
import ProblemAlert from "./ProblemAlert";
import { ApiError, createTransaction, listRecent } from "@/lib/client";
import { newInvoice } from "@/lib/invoice";
import { createSchema, fieldErrors } from "@/lib/schema";
import type { Problem } from "@/lib/types";

let cachedInvoice: string | null = null;
const clientInvoice = () => (cachedInvoice ??= newInvoice());
const noopSubscribe = () => () => {};

export default function CreateForm() {
  const router = useRouter();
  // Server renders "", the browser a generated invoice, without a mismatch or a setState-in-effect.
  const autoInvoice = useSyncExternalStore(noopSubscribe, clientInvoice, () => "");
  const [edited, setInvoice] = useState<string | null>(null);
  const invoice = edited ?? autoInvoice;
  const [amount, setAmount] = useState("12500.00");
  const [phone, setPhone] = useState("08123456789");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [problem, setProblem] = useState<Problem | null>(null);
  const [busy, setBusy] = useState(false);
  const [slow, setSlow] = useState(false);

  async function send(body: { invoice: string; amount: number; phoneNumber: string }, skipLocalCheck = false) {
    setProblem(null);
    if (!skipLocalCheck) {
      const parsed = createSchema.safeParse(body);
      if (!parsed.success) {
        setErrors(fieldErrors(parsed.error));
        return;
      }
    }
    setErrors({});
    setBusy(true);
    const timer = setTimeout(() => setSlow(true), 2000);
    try {
      const tx = await createTransaction(body);
      router.push(`/transactions/${tx.id}`);
    } catch (e) {
      setProblem(e instanceof ApiError ? e.problem : { status: 0, detail: "Couldn't reach the server. Check your connection." });
    } finally {
      clearTimeout(timer);
      setBusy(false);
      setSlow(false);
    }
  }

  async function tryDuplicate() {
    // Reuse the newest existing invoice so the API answers 409.
    try {
      const [latest] = await listRecent(1);
      await send({ invoice: latest?.invoice ?? invoice, amount: 1, phoneNumber: "08123456789" }, true);
    } catch (e) {
      if (e instanceof ApiError) setProblem(e.problem);
    }
  }

  const field = (name: string) =>
    errors[name] ? <p className="mt-1 text-xs text-danger">{errors[name]}</p> : null;

  return (
    <form
      className="card space-y-5 p-5 sm:p-6"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        send({ invoice: invoice.trim(), amount: Number(amount), phoneNumber: phone.trim() });
      }}
    >
      <div className="grid gap-4 sm:grid-cols-3">
        <label className="block text-sm">
          <span className="mb-1 block text-muted">Invoice</span>
          <div className="flex gap-1.5">
            <input id="invoice" className="field font-mono" value={invoice} onChange={(e) => setInvoice(e.target.value)} />
            <button type="button" className="btn px-2.5" aria-label="Generate a new invoice" title="New invoice" onClick={() => setInvoice(newInvoice())}>
              ↻
            </button>
          </div>
          {field("invoice")}
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-muted">Amount (IDR)</span>
          <input id="amount" className="field" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />
          {field("amount")}
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-muted">Phone number</span>
          <input id="phone" className="field" inputMode="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
          {field("phoneNumber")}
        </label>
      </div>

      {problem && <ProblemAlert problem={problem} />}

      <div className="flex flex-wrap items-center gap-2">
        <button type="submit" className="btn btn-primary" disabled={busy || !invoice}>
          {busy ? (slow ? "Waking up the API…" : "Creating…") : "Create transaction"}
        </button>
        <span className="mx-1 hidden h-5 w-px bg-line sm:block" aria-hidden="true" />
        <button type="button" className="btn" disabled={busy} onClick={tryDuplicate}>
          Try a duplicate (409)
        </button>
        <button type="button" className="btn" disabled={busy} onClick={() => send({ invoice: "", amount: -1, phoneNumber: "" }, true)}>
          Try invalid input (400)
        </button>
      </div>
      <p className="text-xs text-faint">
        The two &ldquo;Try&rdquo; buttons skip the form&rsquo;s own checks and send the request anyway, so you see the
        server&rsquo;s RFC 7807 error response. The 409 comes from the Java API; invalid input is rejected by the site&rsquo;s
        server with the same rules before it reaches Java.
      </p>
    </form>
  );
}
