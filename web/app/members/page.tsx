import MemberList from "@/components/MemberList";

export const metadata = { title: "Members · Transaction Demo" };

export default function Page() {
  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <h1 className="text-2xl font-semibold">Members</h1>
        <p className="text-muted">
          A member is created automatically by their first transaction and identified by their phone number, normalized
          to <code className="font-mono text-sm">62…</code>. Numbers are masked here because this list is public.
        </p>
      </div>
      <MemberList />
    </div>
  );
}
