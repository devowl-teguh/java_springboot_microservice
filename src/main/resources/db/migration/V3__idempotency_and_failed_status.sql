-- Design 0001: idempotent creation and a FAILED status. Additive: existing rows keep NULLs.
ALTER TABLE transactions ADD COLUMN idempotency_key VARCHAR(255);
ALTER TABLE transactions ADD COLUMN request_hash VARCHAR(64);
ALTER TABLE transactions ADD COLUMN failure_reason VARCHAR(200);
-- The unique constraint is the source of truth for concurrent requests with the same key.
ALTER TABLE transactions ADD CONSTRAINT transactions_idempotency_key_key UNIQUE (idempotency_key);
ALTER TABLE transactions ADD CONSTRAINT transactions_request_hash_with_key
    CHECK ((idempotency_key IS NULL) = (request_hash IS NULL));
ALTER TABLE transactions ADD CONSTRAINT transactions_status_check
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'));
