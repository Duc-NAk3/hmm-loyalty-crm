-- Hoa Mai Mart Loyalty CRM – demo schema
-- Bám theo tài liệu ERD (tên bảng số nhiều); chỉ gồm 26/42 bảng cần cho 19 FR của bản demo.

CREATE SCHEMA IF NOT EXISTS admin;
CREATE SCHEMA IF NOT EXISTS customer;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS pos;
CREATE SCHEMA IF NOT EXISTS loyalty;
CREATE SCHEMA IF NOT EXISTS migration;
CREATE SCHEMA IF NOT EXISTS shared;

-- ---------------- admin ----------------
CREATE TABLE admin.stores (
    store_id      BIGSERIAL PRIMARY KEY,
    store_code    VARCHAR(10)  NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    region        VARCHAR(50),
    address       VARCHAR(255),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    opened_at     DATE
);

CREATE TABLE admin.pos_terminals (
    terminal_id     BIGSERIAL PRIMARY KEY,
    store_id        BIGINT      NOT NULL REFERENCES admin.stores (store_id),
    terminal_code   VARCHAR(20) NOT NULL UNIQUE,
    device_key_hash VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    registered_at   TIMESTAMPTZ
);

CREATE TABLE admin.roles (
    role_id BIGSERIAL PRIMARY KEY,
    code    VARCHAR(30)  NOT NULL UNIQUE,
    name    VARCHAR(100) NOT NULL
);

CREATE TABLE admin.app_users (
    user_id       BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(10),
    role_id       BIGINT       NOT NULL REFERENCES admin.roles (role_id),
    store_id      BIGINT REFERENCES admin.stores (store_id),
    mfa_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ
);

CREATE TABLE admin.system_parameters (
    param_key   VARCHAR(60) PRIMARY KEY,
    param_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    updated_by  BIGINT REFERENCES admin.app_users (user_id),
    updated_at  TIMESTAMPTZ
);

-- ---------------- loyalty (tiers, policy trước customer vì customers.tier_code tham chiếu tiers) ----------------
CREATE TABLE loyalty.tiers (
    tier_code       VARCHAR(10) PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL,
    threshold_spend BIGINT       NOT NULL,
    multiplier      NUMERIC(3, 2) NOT NULL,
    benefits        TEXT
);

CREATE TABLE loyalty.loyalty_policies (
    policy_id         BIGSERIAL PRIMARY KEY,
    version           INTEGER     NOT NULL UNIQUE,
    earn_unit_vnd     INTEGER     NOT NULL,
    point_value_vnd   INTEGER     NOT NULL,
    min_redeem_points INTEGER     NOT NULL,
    max_redeem_pct    INTEGER     NOT NULL,
    expiry_months     INTEGER     NOT NULL,
    hold_minutes      INTEGER     NOT NULL,
    effective_from    TIMESTAMPTZ NOT NULL,
    status            VARCHAR(15) NOT NULL,
    approved_by       BIGINT REFERENCES admin.app_users (user_id)
);

-- ---------------- customer ----------------
CREATE TABLE customer.customers (
    customer_id         BIGSERIAL PRIMARY KEY,
    member_code         VARCHAR(10)  NOT NULL UNIQUE,
    phone               VARCHAR(10)  NOT NULL,
    full_name           VARCHAR(100) NOT NULL,
    name_search         VARCHAR(100),
    gender              VARCHAR(10),
    date_of_birth       DATE,
    email               VARCHAR(150),
    province_code       VARCHAR(5),
    status              VARCHAR(20)  NOT NULL,
    verified_at         TIMESTAMPTZ,
    tier_code           VARCHAR(10)  NOT NULL REFERENCES loyalty.tiers (tier_code),
    point_balance       INTEGER      NOT NULL DEFAULT 0,
    spend_12m           BIGINT       NOT NULL DEFAULT 0,
    is_employee         BOOLEAN      NOT NULL DEFAULT FALSE,
    redeem_locked       BOOLEAN      NOT NULL DEFAULT FALSE,
    registered_store_id BIGINT REFERENCES admin.stores (store_id),
    merged_into_id      BIGINT REFERENCES customer.customers (customer_id),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ
);
-- SĐT duy nhất trong các hồ sơ chưa bị gộp
CREATE UNIQUE INDEX ux_customers_phone_active ON customer.customers (phone) WHERE status <> 'MERGED';
CREATE INDEX ix_customers_name_search ON customer.customers (name_search);
-- Mã thành viên = HM + 8 chữ số, sinh từ sequence, không đổi (BRL-01)
CREATE SEQUENCE customer.member_code_seq START 10000001;

CREATE TABLE customer.consents (
    consent_id    BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT      NOT NULL REFERENCES customer.customers (customer_id),
    purpose       VARCHAR(30) NOT NULL,
    status        VARCHAR(15) NOT NULL,
    channel       VARCHAR(20),
    terms_version VARCHAR(20),
    captured_by   BIGINT REFERENCES admin.app_users (user_id),
    captured_at   TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_consents_customer ON customer.consents (customer_id);

-- ---------------- catalog ----------------
CREATE TABLE catalog.categories (
    category_id      BIGSERIAL PRIMARY KEY,
    code             VARCHAR(20)  NOT NULL UNIQUE,
    name             VARCHAR(100) NOT NULL,
    parent_id        BIGINT REFERENCES catalog.categories (category_id),
    promo_restricted BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE catalog.products (
    product_id       BIGSERIAL PRIMARY KEY,
    sku              VARCHAR(30)  NOT NULL UNIQUE,
    name             VARCHAR(200) NOT NULL,
    unit             VARCHAR(20)  NOT NULL,
    category_id      BIGINT       NOT NULL REFERENCES catalog.categories (category_id),
    vat_rate         NUMERIC(4, 2) NOT NULL,
    promo_restricted BOOLEAN      NOT NULL DEFAULT FALSE,
    point_excluded   BOOLEAN      NOT NULL DEFAULT FALSE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
);
CREATE INDEX ix_products_name ON catalog.products (lower(name));

CREATE TABLE catalog.product_barcodes (
    barcode    VARCHAR(20) PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES catalog.products (product_id)
);
CREATE INDEX ix_product_barcodes_product ON catalog.product_barcodes (product_id);

CREATE TABLE catalog.product_prices (
    price_id       BIGSERIAL PRIMARY KEY,
    product_id     BIGINT      NOT NULL REFERENCES catalog.products (product_id),
    store_id       BIGINT REFERENCES admin.stores (store_id),
    price          BIGINT      NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to   TIMESTAMPTZ,
    status         VARCHAR(20) NOT NULL,
    approved_by    BIGINT REFERENCES admin.app_users (user_id)
);
CREATE INDEX ix_product_prices_lookup ON catalog.product_prices (product_id, store_id, effective_from);

-- ---------------- pos ----------------
CREATE TABLE pos.shifts (
    shift_id        BIGSERIAL PRIMARY KEY,
    terminal_id     BIGINT      NOT NULL REFERENCES admin.pos_terminals (terminal_id),
    cashier_id      BIGINT      NOT NULL REFERENCES admin.app_users (user_id),
    opened_at       TIMESTAMPTZ NOT NULL,
    opening_cash    BIGINT      NOT NULL,
    closed_at       TIMESTAMPTZ,
    expected_cash   BIGINT,
    counted_cash    BIGINT,
    cash_difference BIGINT,
    confirmed_by    BIGINT REFERENCES admin.app_users (user_id)
);
-- mỗi quầy chỉ có một ca đang mở (FR-01)
CREATE UNIQUE INDEX ux_shifts_one_open ON pos.shifts (terminal_id) WHERE closed_at IS NULL;

CREATE TABLE pos.sales_transactions (
    transaction_id  BIGSERIAL PRIMARY KEY,
    receipt_no      VARCHAR(30) NOT NULL UNIQUE,
    store_id        BIGINT      NOT NULL REFERENCES admin.stores (store_id),
    terminal_id     BIGINT REFERENCES admin.pos_terminals (terminal_id),
    shift_id        BIGINT REFERENCES pos.shifts (shift_id),
    cashier_id      BIGINT REFERENCES admin.app_users (user_id),
    customer_id     BIGINT REFERENCES customer.customers (customer_id),
    captured_phone  VARCHAR(10),
    txn_type        VARCHAR(10) NOT NULL,
    original_txn_id BIGINT REFERENCES pos.sales_transactions (transaction_id),
    status          VARCHAR(15) NOT NULL,
    source          VARCHAR(15) NOT NULL,
    txn_time        TIMESTAMPTZ NOT NULL,
    synced_at       TIMESTAMPTZ,
    net_amount      BIGINT      NOT NULL,
    discount_amount BIGINT      NOT NULL DEFAULT 0,
    eligible_amount BIGINT,
    points_earned   INTEGER,
    points_redeemed INTEGER
);
CREATE INDEX ix_sales_transactions_time ON pos.sales_transactions (txn_time);
CREATE INDEX ix_sales_transactions_customer ON pos.sales_transactions (customer_id);
CREATE INDEX ix_sales_transactions_store_time ON pos.sales_transactions (store_id, txn_time);

CREATE TABLE pos.transaction_lines (
    line_id        BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT         NOT NULL REFERENCES pos.sales_transactions (transaction_id),
    product_id     BIGINT         NOT NULL REFERENCES catalog.products (product_id),
    quantity       NUMERIC(10, 3) NOT NULL,
    unit_price     BIGINT         NOT NULL,
    discount       BIGINT         NOT NULL DEFAULT 0,
    line_amount    BIGINT         NOT NULL,
    point_eligible BOOLEAN        NOT NULL
);
CREATE INDEX ix_transaction_lines_txn ON pos.transaction_lines (transaction_id);

-- ---------------- loyalty ----------------
CREATE TABLE loyalty.point_redemptions (
    redemption_id   BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT      NOT NULL REFERENCES customer.customers (customer_id),
    terminal_id     BIGINT      NOT NULL REFERENCES admin.pos_terminals (terminal_id),
    transaction_id  BIGINT REFERENCES pos.sales_transactions (transaction_id),
    points          INTEGER     NOT NULL,
    amount_vnd      BIGINT      NOT NULL,
    status          VARCHAR(15) NOT NULL,
    otp_verified_at TIMESTAMPTZ,
    hold_expires_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_point_redemptions_status ON loyalty.point_redemptions (status, hold_expires_at);

CREATE TABLE pos.payments (
    payment_id     BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT      NOT NULL REFERENCES pos.sales_transactions (transaction_id),
    method         VARCHAR(15) NOT NULL,
    amount         BIGINT      NOT NULL,
    redemption_id  BIGINT REFERENCES loyalty.point_redemptions (redemption_id),
    voucher_id     BIGINT,  -- bảng vouchers không thuộc bản demo
    reference      VARCHAR(50)
);
CREATE INDEX ix_payments_txn ON pos.payments (transaction_id);

CREATE TABLE loyalty.point_lots (
    lot_id           BIGSERIAL PRIMARY KEY,
    customer_id      BIGINT      NOT NULL REFERENCES customer.customers (customer_id),
    transaction_id   BIGINT REFERENCES pos.sales_transactions (transaction_id),
    earned_at        TIMESTAMPTZ NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    points_earned    INTEGER     NOT NULL,
    points_remaining INTEGER     NOT NULL
);
CREATE INDEX ix_point_lots_fifo ON loyalty.point_lots (customer_id, earned_at) WHERE points_remaining > 0;

CREATE TABLE loyalty.point_ledgers (
    ledger_id      BIGSERIAL PRIMARY KEY,
    customer_id    BIGINT      NOT NULL REFERENCES customer.customers (customer_id),
    entry_type     VARCHAR(10) NOT NULL,
    points         INTEGER     NOT NULL,
    balance_after  INTEGER     NOT NULL,
    lot_id         BIGINT REFERENCES loyalty.point_lots (lot_id),
    transaction_id BIGINT REFERENCES pos.sales_transactions (transaction_id),
    redemption_id  BIGINT REFERENCES loyalty.point_redemptions (redemption_id),
    adjustment_id  BIGINT,  -- bảng point_adjustments không thuộc bản demo
    policy_id      BIGINT      NOT NULL REFERENCES loyalty.loyalty_policies (policy_id),
    created_by     BIGINT REFERENCES admin.app_users (user_id),
    created_at     TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_point_ledgers_customer ON loyalty.point_ledgers (customer_id, ledger_id);

-- Sổ cái chỉ ghi thêm: chặn UPDATE / DELETE ở mức CSDL (FR-33)
CREATE FUNCTION loyalty.forbid_ledger_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'point_ledgers chỉ được ghi thêm, không sửa hoặc xóa';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_point_ledgers_append_only BEFORE UPDATE OR DELETE ON loyalty.point_ledgers
    FOR EACH ROW EXECUTE FUNCTION loyalty.forbid_ledger_change();

CREATE TABLE loyalty.tier_histories (
    history_id  BIGSERIAL PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customer.customers (customer_id),
    from_tier   VARCHAR(10) REFERENCES loyalty.tiers (tier_code),
    to_tier     VARCHAR(10) NOT NULL REFERENCES loyalty.tiers (tier_code),
    reason      VARCHAR(15) NOT NULL,
    changed_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_tier_histories_customer ON loyalty.tier_histories (customer_id, changed_at);

-- ---------------- migration ----------------
CREATE TABLE migration.import_batches (
    batch_id      BIGSERIAL PRIMARY KEY,
    store_id      BIGINT       NOT NULL REFERENCES admin.stores (store_id),
    source_vendor VARCHAR(30)  NOT NULL,
    data_type     VARCHAR(15)  NOT NULL,
    file_path     VARCHAR(255) NOT NULL,
    status        VARCHAR(15)  NOT NULL,
    total_rows    INTEGER,
    error_rows    INTEGER,
    imported_by   BIGINT REFERENCES admin.app_users (user_id),
    imported_at   TIMESTAMPTZ
);

CREATE TABLE migration.import_errors (
    error_id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT       NOT NULL REFERENCES migration.import_batches (batch_id),
    row_no   INTEGER      NOT NULL,
    message  VARCHAR(255) NOT NULL,
    raw_data JSONB
);

CREATE TABLE migration.customer_source_records (
    source_id           BIGSERIAL PRIMARY KEY,
    customer_id         BIGINT      NOT NULL REFERENCES customer.customers (customer_id),
    batch_id            BIGINT      NOT NULL REFERENCES migration.import_batches (batch_id),
    source_vendor       VARCHAR(30) NOT NULL,
    source_customer_ref VARCHAR(50),
    raw_data            JSONB
);

-- ---------------- shared ----------------
CREATE TABLE shared.outbox_messages (
    outbox_id       BIGSERIAL PRIMARY KEY,
    message_type    VARCHAR(30) NOT NULL,
    aggregate_type  VARCHAR(30) NOT NULL,
    aggregate_id    BIGINT      NOT NULL,
    payload         JSONB       NOT NULL,
    status          VARCHAR(10) NOT NULL,
    attempts        INTEGER     NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    sent_at         TIMESTAMPTZ
);
CREATE INDEX ix_outbox_messages_status ON shared.outbox_messages (status, next_attempt_at);

CREATE TABLE shared.audit_logs (
    audit_id    BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES admin.app_users (user_id),
    action      VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40),
    entity_id   VARCHAR(40),
    before_data JSONB,
    after_data  JSONB,
    created_at  TIMESTAMPTZ NOT NULL
);
