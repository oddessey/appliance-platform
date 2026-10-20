CREATE TABLE technician_availability (
    id uuid PRIMARY KEY,
    technician_id uuid NOT NULL REFERENCES technician_profile (id) ON DELETE CASCADE,
    day_of_week smallint NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time time NOT NULL,
    end_time time NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (end_time > start_time),
    UNIQUE (technician_id, day_of_week, start_time, end_time)
);
CREATE INDEX ix_technician_availability_day_time
    ON technician_availability (day_of_week, start_time, end_time)
    WHERE active = true;
