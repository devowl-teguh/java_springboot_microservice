-- When the outbox relay handed the event to the broker, and when the consumer completed it.
-- Nullable: rows created before this migration simply have no timestamps.
ALTER TABLE transactions ADD COLUMN published_at TIMESTAMPTZ;
ALTER TABLE transactions ADD COLUMN completed_at TIMESTAMPTZ;
-- Supports GET /transactions (newest first).
CREATE INDEX transactions_created_at_idx ON transactions (created_at DESC);
