ALTER TABLE reservation
    ADD COLUMN idempotency_key varchar(100),
    ADD COLUMN idempotency_request_hash varchar(64),
    ADD CONSTRAINT ck_reservation_idempotency_pair CHECK (
        (idempotency_key IS NULL AND idempotency_request_hash IS NULL)
        OR (idempotency_key IS NOT NULL AND idempotency_request_hash IS NOT NULL)
    );

CREATE UNIQUE INDEX uq_reservation_idempotency_key
    ON reservation (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX uq_reservation_active_move_request
    ON reservation (move_request_id)
    WHERE status IN ('PENDING', 'CONFIRMED', 'RESCHEDULE_REQUESTED');
