import Lookup from "@/components/Lookup";

export const metadata = { title: "Look up · Transaction Demo" };

export default function Page() {
  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <h1 className="text-2xl font-semibold">Look up a transaction</h1>
        <p className="text-muted">Paste a transaction ID, or open one of the most recent transactions.</p>
      </div>
      <Lookup />
    </div>
  );
}
