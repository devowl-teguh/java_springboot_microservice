import { REPO_URL } from "@/components/Nav";

export const metadata = { title: "How it works · Transaction Demo" };

const SWAGGER_URL = "https://szvtm226kimgrfzuc2etloccne0dcrmt.lambda-url.us-east-1.on.aws/swagger-ui/index.html";

const flow = [
  { name: "This website", note: "Next.js on Vercel; its API routes proxy to the Java API" },
  { name: "ApiFunction", note: "Spring Boot REST API on AWS Lambda (Function URL)" },
  { name: "Neon Postgres", note: "transactions + outbox written in one commit" },
  { name: "OutboxFunction", note: "every minute via EventBridge Scheduler, sends to SQS" },
  { name: "Amazon SQS", note: "durable queue with a dead-letter queue after 3 failures" },
  { name: "ConsumerFunction", note: "marks the transaction SUCCESS (idempotent)" },
];

const modes = [
  ["API", "Spring Boot on port 8080", "Spring Boot inside a Lambda function"],
  ["Outbox relay", "@Scheduled every second", "Lambda every minute (EventBridge)"],
  ["Broker", "Apache Kafka", "Amazon SQS"],
  ["Dead letters", "Kafka DLT topic", "SQS dead-letter queue"],
  ["Cost", "Your machine", "About $0 on the free tiers"],
];

export default function Page() {
  return (
    <article className="space-y-8">
      <div className="space-y-2">
        <h1 className="text-2xl font-semibold">How it works</h1>
        <p className="text-muted">
          A Java 21 / Spring Boot service that accepts transactions over REST and processes them asynchronously. It
          shows the <b className="font-medium text-fg">transactional outbox</b> pattern: the transaction and its event
          are saved together, so an event is never lost even if the message broker is down.
        </p>
      </div>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">The flow of one transaction</h2>
        <ol className="card divide-y divide-line">
          {flow.map((step, i) => (
            <li key={step.name} className="flex gap-3 px-4 py-3 text-sm">
              <span className="flex size-6 flex-none items-center justify-center rounded-full bg-accent-soft text-xs font-medium text-accent">{i + 1}</span>
              <div>
                <p className="font-medium">{step.name}</p>
                <p className="text-muted">{step.note}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Two runtimes, one codebase</h2>
        <p className="text-muted">
          Messaging sits behind one interface, <code className="rounded bg-sunk px-1 font-mono text-sm">TransactionEventPublisher</code>.
          The default profile plugs in Kafka; the <code className="rounded bg-sunk px-1 font-mono text-sm">aws</code> profile plugs in
          SQS and runs on Lambda for free. The business logic does not change.
        </p>
        <div className="overflow-x-auto">
          <table className="card w-full min-w-[30rem] text-left text-sm">
            <thead className="text-muted">
              <tr className="border-b border-line">
                <th className="px-4 py-2 font-medium"></th>
                <th className="px-4 py-2 font-medium">Kafka mode (local)</th>
                <th className="px-4 py-2 font-medium">AWS mode (this demo)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {modes.map(([k, a, b]) => (
                <tr key={k}>
                  <td className="px-4 py-2 font-medium">{k}</td>
                  <td className="px-4 py-2 text-muted">{a}</td>
                  <td className="px-4 py-2 text-muted">{b}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Reliability details</h2>
        <ul className="list-disc space-y-1.5 pl-5 text-muted">
          <li><b className="font-medium text-fg">At-least-once delivery:</b> an event can arrive twice; processing only sets the same final status, so duplicates are harmless.</li>
          <li><b className="font-medium text-fg">No double sends between relays:</b> the relay locks rows with <code className="font-mono text-sm">FOR UPDATE SKIP LOCKED</code>.</li>
          <li><b className="font-medium text-fg">Poison messages:</b> after 3 failed attempts a message moves to a dead-letter queue instead of blocking others.</li>
          <li><b className="font-medium text-fg">Consistent errors:</b> every error is RFC 7807 <code className="font-mono text-sm">application/problem+json</code> with field-level messages.</li>
          <li><b className="font-medium text-fg">Tested:</b> unit tests in CI, plus Testcontainers tests against real PostgreSQL and Kafka, and Lambda handler tests.</li>
        </ul>
      </section>

      <section className="flex flex-wrap gap-2">
        <a className="btn" href={REPO_URL} target="_blank" rel="noreferrer">Source on GitHub ↗</a>
        <a className="btn" href={SWAGGER_URL} target="_blank" rel="noreferrer">Swagger UI ↗</a>
      </section>
    </article>
  );
}
