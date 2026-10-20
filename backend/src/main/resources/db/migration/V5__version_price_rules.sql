ALTER TABLE price_rule
    ADD COLUMN version integer NOT NULL DEFAULT 1 CHECK (version >= 1);

CREATE INDEX ix_quote_status_expiry ON quote (status, expires_at);

CREATE INDEX ix_move_request_submitted_created
    ON move_request (status, created_at DESC)
    WHERE status IN ('SUBMITTED', 'QUOTED');
