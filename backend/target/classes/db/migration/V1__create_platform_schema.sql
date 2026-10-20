CREATE TABLE app_user (
    id uuid PRIMARY KEY,
    email varchar(254) NOT NULL,
    password_hash varchar(255) NOT NULL,
    name varchar(100) NOT NULL,
    phone varchar(30) NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN')),
    email_verified_at timestamptz,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE UNIQUE INDEX uq_app_user_email_lower ON app_user (lower(email));

CREATE TABLE user_role (
    user_id uuid NOT NULL REFERENCES app_user (id),
    role_code varchar(20) NOT NULL CHECK (role_code IN ('CUSTOMER', 'TECHNICIAN', 'ADMIN')),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_code)
);

CREATE TABLE appliance_type (
    id uuid PRIMARY KEY,
    code varchar(40) NOT NULL UNIQUE,
    name varchar(100) NOT NULL,
    description text,
    active boolean NOT NULL DEFAULT true,
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE move_request (
    id uuid PRIMARY KEY,
    request_no varchar(30) NOT NULL UNIQUE,
    customer_id uuid NOT NULL REFERENCES app_user (id),
    status varchar(30) NOT NULL CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'ANALYZING', 'NEEDS_REVIEW', 'QUOTED',
        'RESERVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
    )),
    origin_address varchar(500) NOT NULL,
    origin_address_detail varchar(300),
    destination_address varchar(500) NOT NULL,
    destination_address_detail varchar(300),
    desired_start_at timestamptz NOT NULL,
    desired_end_at timestamptz,
    customer_memo text,
    submitted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (desired_end_at IS NULL OR desired_end_at > desired_start_at)
);
CREATE INDEX ix_move_request_customer_created ON move_request (customer_id, created_at DESC);
CREATE INDEX ix_move_request_status_created ON move_request (status, created_at);
CREATE INDEX ix_move_request_status_desired_start ON move_request (status, desired_start_at);

CREATE TABLE move_item (
    id uuid PRIMARY KEY,
    move_request_id uuid NOT NULL REFERENCES move_request (id),
    appliance_type_id uuid NOT NULL REFERENCES appliance_type (id),
    manufacturer varchar(100),
    model_name varchar(150),
    quantity integer NOT NULL CHECK (quantity >= 1),
    width_mm integer CHECK (width_mm IS NULL OR width_mm > 0),
    height_mm integer CHECK (height_mm IS NULL OR height_mm > 0),
    depth_mm integer CHECK (depth_mm IS NULL OR depth_mm > 0),
    installation_conditions jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_move_item_move_request ON move_item (move_request_id);
CREATE INDEX ix_move_item_appliance_type ON move_item (appliance_type_id);

CREATE TABLE request_image (
    id uuid PRIMARY KEY,
    move_request_id uuid NOT NULL REFERENCES move_request (id),
    move_item_id uuid REFERENCES move_item (id),
    storage_provider varchar(20) NOT NULL CHECK (storage_provider IN ('S3', 'AZURE_BLOB', 'LOCAL')),
    object_key varchar(1000) NOT NULL,
    original_filename varchar(255) NOT NULL,
    content_type varchar(100) NOT NULL,
    size_bytes bigint NOT NULL CHECK (size_bytes > 0),
    purpose varchar(30) NOT NULL CHECK (purpose IN ('APPLIANCE', 'INSTALLATION_ENV', 'WORK_PROOF')),
    uploaded_by uuid NOT NULL REFERENCES app_user (id),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_request_image_move_request ON request_image (move_request_id);
CREATE INDEX ix_request_image_move_item ON request_image (move_item_id);
CREATE INDEX ix_request_image_uploaded_by ON request_image (uploaded_by);

CREATE TABLE ai_analysis (
    id uuid PRIMARY KEY,
    move_request_id uuid NOT NULL REFERENCES move_request (id),
    status varchar(20) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'NEEDS_REVIEW')),
    provider varchar(40) NOT NULL,
    model_name varchar(100),
    input_snapshot jsonb NOT NULL,
    result_json jsonb,
    confidence numeric(5, 4) CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
    error_code varchar(100),
    error_message text,
    started_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
);
CREATE INDEX ix_ai_analysis_move_request_created ON ai_analysis (move_request_id, created_at DESC);
CREATE INDEX ix_ai_analysis_status_created ON ai_analysis (status, created_at);

CREATE TABLE price_rule (
    id uuid PRIMARY KEY,
    rule_code varchar(60) NOT NULL UNIQUE,
    name varchar(150) NOT NULL,
    description text,
    conditions jsonb NOT NULL,
    calculation_type varchar(20) NOT NULL,
    amount numeric(12, 2) NOT NULL CHECK (amount >= 0),
    priority integer NOT NULL DEFAULT 0,
    active boolean NOT NULL DEFAULT true,
    valid_from timestamptz,
    valid_to timestamptz,
    created_by uuid NOT NULL REFERENCES app_user (id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to > valid_from)
);
CREATE INDEX ix_price_rule_active_priority ON price_rule (active, priority);
CREATE INDEX ix_price_rule_created_by ON price_rule (created_by);

CREATE TABLE quote (
    id uuid PRIMARY KEY,
    quote_no varchar(30) NOT NULL UNIQUE,
    move_request_id uuid NOT NULL REFERENCES move_request (id),
    price_rule_id uuid REFERENCES price_rule (id),
    version_no integer NOT NULL CHECK (version_no >= 1),
    status varchar(20) NOT NULL CHECK (status IN ('DRAFT', 'ISSUED', 'ACCEPTED', 'EXPIRED', 'CANCELLED')),
    subtotal_amount numeric(12, 2) NOT NULL CHECK (subtotal_amount >= 0),
    discount_amount numeric(12, 2) NOT NULL CHECK (discount_amount >= 0),
    tax_amount numeric(12, 2) NOT NULL CHECK (tax_amount >= 0),
    total_amount numeric(12, 2) NOT NULL CHECK (total_amount >= 0),
    currency char(3) NOT NULL DEFAULT 'KRW',
    calculation_snapshot jsonb NOT NULL,
    issued_at timestamptz,
    expires_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (move_request_id, version_no),
    UNIQUE (id, move_request_id),
    CHECK (expires_at IS NULL OR issued_at IS NULL OR expires_at > issued_at)
);
CREATE INDEX ix_quote_move_request ON quote (move_request_id);
CREATE INDEX ix_quote_price_rule ON quote (price_rule_id);

CREATE TABLE quote_line (
    id uuid PRIMARY KEY,
    quote_id uuid NOT NULL REFERENCES quote (id),
    line_code varchar(60) NOT NULL,
    description varchar(255) NOT NULL,
    quantity numeric(10, 2) NOT NULL CHECK (quantity > 0),
    unit_price numeric(12, 2) NOT NULL CHECK (unit_price >= 0),
    line_amount numeric(12, 2) NOT NULL CHECK (line_amount >= 0),
    rule_snapshot jsonb
);
CREATE INDEX ix_quote_line_quote ON quote_line (quote_id);

CREATE TABLE reservation (
    id uuid PRIMARY KEY,
    reservation_no varchar(30) NOT NULL UNIQUE,
    move_request_id uuid NOT NULL REFERENCES move_request (id),
    quote_id uuid NOT NULL,
    status varchar(30) NOT NULL CHECK (status IN (
        'PENDING', 'CONFIRMED', 'RESCHEDULE_REQUESTED', 'CANCELLED', 'EXPIRED', 'COMPLETED'
    )),
    scheduled_start_at timestamptz NOT NULL,
    scheduled_end_at timestamptz NOT NULL,
    customer_memo text,
    confirmed_at timestamptz,
    cancelled_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (quote_id, move_request_id) REFERENCES quote (id, move_request_id),
    CHECK (scheduled_end_at > scheduled_start_at)
);
CREATE INDEX ix_reservation_request ON reservation (move_request_id);
CREATE INDEX ix_reservation_quote ON reservation (quote_id);
CREATE INDEX ix_reservation_status_start ON reservation (status, scheduled_start_at);

CREATE TABLE payment (
    id uuid PRIMARY KEY,
    reservation_id uuid NOT NULL REFERENCES reservation (id),
    payment_provider varchar(30) NOT NULL,
    order_id varchar(100) NOT NULL UNIQUE,
    payment_key varchar(200),
    amount numeric(12, 2) NOT NULL CHECK (amount >= 0),
    status varchar(30) NOT NULL CHECK (status IN (
        'READY', 'IN_PROGRESS', 'PAID', 'FAILED', 'CANCEL_REQUESTED',
        'CANCELLED', 'PARTIALLY_REFUNDED', 'REFUNDED'
    )),
    method varchar(40),
    approved_at timestamptz,
    cancelled_amount numeric(12, 2) NOT NULL DEFAULT 0 CHECK (cancelled_amount >= 0 AND cancelled_amount <= amount),
    idempotency_key varchar(100) NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_payment_payment_key ON payment (payment_key) WHERE payment_key IS NOT NULL;
CREATE INDEX ix_payment_reservation ON payment (reservation_id);

CREATE TABLE payment_event (
    id uuid PRIMARY KEY,
    payment_id uuid NOT NULL REFERENCES payment (id),
    provider_event_id varchar(200),
    event_type varchar(60) NOT NULL,
    payload_summary jsonb,
    processed_at timestamptz,
    processing_status varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_payment_event_provider_event_id
    ON payment_event (provider_event_id) WHERE provider_event_id IS NOT NULL;
CREATE INDEX ix_payment_event_payment_created ON payment_event (payment_id, created_at);

CREATE TABLE technician_profile (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE REFERENCES app_user (id),
    approval_status varchar(30) NOT NULL,
    business_name varchar(150),
    license_info text,
    active boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE technician_service_area (
    id uuid PRIMARY KEY,
    technician_id uuid NOT NULL REFERENCES technician_profile (id),
    region_code varchar(30) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (technician_id, region_code)
);
CREATE INDEX ix_technician_service_area_region ON technician_service_area (region_code);

CREATE TABLE technician_capability (
    technician_id uuid NOT NULL REFERENCES technician_profile (id),
    appliance_type_id uuid NOT NULL REFERENCES appliance_type (id),
    active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (technician_id, appliance_type_id)
);
CREATE INDEX ix_technician_capability_appliance ON technician_capability (appliance_type_id);

CREATE TABLE technician_assignment (
    id uuid PRIMARY KEY,
    reservation_id uuid NOT NULL REFERENCES reservation (id),
    technician_id uuid NOT NULL REFERENCES technician_profile (id),
    status varchar(30) NOT NULL,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    responded_at timestamptz,
    completed_at timestamptz,
    rejection_reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_technician_assignment_reservation ON technician_assignment (reservation_id);
CREATE INDEX ix_technician_assignment_technician_status ON technician_assignment (technician_id, status);

CREATE TABLE work_event (
    id uuid PRIMARY KEY,
    assignment_id uuid NOT NULL REFERENCES technician_assignment (id),
    event_type varchar(40) NOT NULL,
    note text,
    evidence_image_id uuid REFERENCES request_image (id),
    created_by uuid NOT NULL REFERENCES app_user (id),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_work_event_assignment_created ON work_event (assignment_id, created_at);
CREATE INDEX ix_work_event_evidence_image ON work_event (evidence_image_id);
CREATE INDEX ix_work_event_created_by ON work_event (created_by);

CREATE TABLE review (
    id uuid PRIMARY KEY,
    reservation_id uuid NOT NULL UNIQUE REFERENCES reservation (id),
    customer_id uuid NOT NULL REFERENCES app_user (id),
    rating integer NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_review_customer ON review (customer_id);

CREATE TABLE audit_log (
    id uuid PRIMARY KEY,
    actor_user_id uuid REFERENCES app_user (id),
    action varchar(100) NOT NULL,
    entity_type varchar(100) NOT NULL,
    entity_id uuid,
    request_id varchar(100),
    before_summary jsonb,
    after_summary jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_log_entity ON audit_log (entity_type, entity_id, created_at);
CREATE INDEX ix_audit_log_actor_created ON audit_log (actor_user_id, created_at);
