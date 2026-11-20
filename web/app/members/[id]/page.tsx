import { Suspense } from "react";
import MemberDetailView from "@/components/MemberDetailView";

// The ID is only known at request time, so reading params happens inside Suspense (cacheComponents is on).
export default function Page({ params }: PageProps<"/members/[id]">) {
  return (
    <Suspense fallback={<p className="text-muted">Loading member…</p>}>
      <Detail params={params} />
    </Suspense>
  );
}

async function Detail({ params }: Pick<PageProps<"/members/[id]">, "params">) {
  const { id } = await params;
  return <MemberDetailView id={id} />;
}
