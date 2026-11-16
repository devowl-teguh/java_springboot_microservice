import { Suspense } from "react";
import TransactionDetail from "@/components/TransactionDetail";

// The ID is only known at request time, so reading params happens inside Suspense (cacheComponents is on).
export default function Page({ params }: PageProps<"/transactions/[id]">) {
  return (
    <Suspense fallback={<p className="text-muted">Loading transaction…</p>}>
      <Detail params={params} />
    </Suspense>
  );
}

async function Detail({ params }: Pick<PageProps<"/transactions/[id]">, "params">) {
  const { id } = await params;
  return <TransactionDetail id={id} />;
}
