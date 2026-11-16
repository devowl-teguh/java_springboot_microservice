import type { Status } from "@/lib/types";

export default function StatusBadge({ status }: { status: Status }) {
  const style = status === "SUCCESS" ? "bg-success-soft text-success" : "bg-pending-soft text-pending";
  return <span className={`rounded-md px-2.5 py-0.5 text-xs font-medium tracking-wide ${style}`}>{status}</span>;
}
