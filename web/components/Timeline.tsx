import { duration, formatTime } from "@/lib/format";
import type { Transaction } from "@/lib/types";

type State = "done" | "now" | "todo";

/** The four stages of the outbox flow, driven by the API's createdAt / publishedAt / completedAt. */
export function stages(tx: Transaction) {
  const published = Boolean(tx.publishedAt);
  const done = tx.status === "SUCCESS";
  return [
    {
      title: "Saved as PENDING",
      body: "Transaction and outbox row committed in one database transaction.",
      at: tx.createdAt,
      state: "done" as State,
    },
    {
      title: published || done ? "Picked up by the outbox relay" : "Waiting for the outbox relay",
      body: "A Lambda runs every minute on EventBridge Scheduler and sends pending events to SQS.",
      at: tx.publishedAt,
      state: (published || done ? "done" : "now") as State,
    },
    {
      title: "Delivered by SQS",
      body: "SQS triggers the consumer Lambda; failed messages retry, then go to a dead-letter queue.",
      at: null,
      state: (done ? "done" : published ? "now" : "todo") as State,
    },
    {
      title: done ? "Processed: SUCCESS" : "Processing",
      body: "The consumer marks the transaction SUCCESS. Redelivery is safe because processing is idempotent.",
      at: tx.completedAt,
      state: (done ? "done" : "todo") as State,
    },
  ];
}

export default function Timeline({ tx }: { tx: Transaction }) {
  return (
    <ol className="space-y-1">
      {stages(tx).map((s, i) => (
        <li key={s.title} className="flex gap-3 py-1.5">
          <span
            aria-hidden="true"
            className={`mt-0.5 flex size-6 flex-none items-center justify-center rounded-full border text-xs ${
              s.state === "done"
                ? "border-success/40 bg-success-soft text-success"
                : s.state === "now"
                  ? "border-accent/40 bg-accent-soft text-accent"
                  : "border-line text-faint"
            }`}
          >
            {s.state === "done" ? "✓" : s.state === "now" ? <span className="size-2 animate-pulse rounded-full bg-accent" /> : i + 1}
          </span>
          <div className="min-w-0 text-sm">
            <p className={s.state === "todo" ? "text-muted" : "font-medium"}>
              {s.title}
              <span className="sr-only">{s.state === "done" ? " (done)" : s.state === "now" ? " (in progress)" : ""}</span>
            </p>
            <p className="text-muted">{s.body}</p>
            {s.at && (
              <p className="mt-0.5 font-mono text-xs text-faint">
                {formatTime(s.at)} · +{duration(tx.createdAt, s.at)}
              </p>
            )}
          </div>
        </li>
      ))}
    </ol>
  );
}
