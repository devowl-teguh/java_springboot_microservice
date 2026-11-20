-- Design 0002: members identified by a normalized Indonesian phone number. Additive; no backfill.
CREATE TABLE members (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT members_phone_number_key UNIQUE (phone_number),
    CONSTRAINT members_phone_number_format CHECK (phone_number ~ '^62[1-9][0-9]{6,11}$')
);
CREATE INDEX members_created_at_idx ON members (created_at DESC);

-- Nullable: transactions created before this migration have no member.
ALTER TABLE transactions ADD COLUMN member_id UUID REFERENCES members (id);
CREATE INDEX transactions_member_id_created_at_idx ON transactions (member_id, created_at DESC);
