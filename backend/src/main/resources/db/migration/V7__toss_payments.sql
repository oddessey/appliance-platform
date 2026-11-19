ALTER TABLE payment
    ALTER COLUMN payment_key TYPE varchar(200),
    ADD COLUMN IF NOT EXISTS toss_response jsonb,
    ADD COLUMN IF NOT EXISTS paid_at timestamptz;

CREATE INDEX IF NOT EXISTS ix_payment_order ON payment (order_id);
CREATE INDEX IF NOT EXISTS ix_payment_status ON payment (status);
