import type { Problem } from "@/lib/types";

/** Renders an RFC 7807 error from the API: status, detail and per-field messages. */
export default function ProblemAlert({ problem }: { problem: Problem }) {
  const fields = Object.entries(problem.errors ?? {});
  return (
    <div role="alert" className="rounded-lg border border-danger/30 bg-danger-soft px-4 py-3 text-sm text-danger">
      <p className="font-medium">
        {problem.status} {problem.title ?? ""}
        {problem.detail ? `: ${problem.detail}` : ""}
      </p>
      {fields.length > 0 && (
        <ul className="mt-1 list-disc pl-5">
          {fields.map(([field, message]) => (
            <li key={field}>
              <span className="font-mono">{field}</span> {message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
