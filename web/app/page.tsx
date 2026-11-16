import CreateForm from "@/components/CreateForm";

export default function Home() {
  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <h1 className="text-2xl font-semibold text-balance">Create a transaction</h1>
        <p className="text-muted">
          Watch it move from <b className="font-medium text-pending">PENDING</b> to{" "}
          <b className="font-medium text-success">SUCCESS</b> through a transactional outbox, Amazon SQS and an AWS
          Lambda consumer. The invoice is pre-filled with a unique value.
        </p>
      </div>
      <CreateForm />
    </div>
  );
}
