-- V5__paid_consultation_esewa_and_pricing.sql
-- Production database migration for Paid Consultation, Expert Packages, Platform Commission & eSewa Payment

-- 1. Consultation Packages
CREATE TABLE IF NOT EXISTS consultation_packages (
    id BIGSERIAL PRIMARY KEY,
    expert_id BIGINT NOT NULL,
    crop_id BIGINT,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'NPR',
    duration_hours INT NOT NULL DEFAULT 168, -- default 7 days (168 hours)
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cp_expert FOREIGN KEY (expert_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_crop FOREIGN KEY (crop_id) REFERENCES crops(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_cp_expert_active ON consultation_packages(expert_id, is_active);
CREATE INDEX IF NOT EXISTS idx_cp_crop_active ON consultation_packages(crop_id, is_active);

-- 2. Upgrade consultations table with package, price snapshot, and lifecycle timestamps
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS package_id BIGINT;
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS price_at_purchase NUMERIC(12, 2);
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'NPR';
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS payment_verified_at TIMESTAMP;
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS started_at TIMESTAMP;
ALTER TABLE consultations ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_consultations_package'
    ) THEN
        ALTER TABLE consultations
        ADD CONSTRAINT fk_consultations_package FOREIGN KEY (package_id) REFERENCES consultation_packages(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_consultations_expires_at ON consultations(expires_at);

-- 3. Payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    payer_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL DEFAULT 'ESEWA',
    provider_transaction_id VARCHAR(100),
    provider_reference_id VARCHAR(100),
    transaction_uuid VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'NPR',
    platform_commission NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    expert_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    raw_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    failed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_consultation FOREIGN KEY (consultation_id) REFERENCES consultations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_payer FOREIGN KEY (payer_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_payments_consultation ON payments(consultation_id);
CREATE INDEX IF NOT EXISTS idx_payments_payer ON payments(payer_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_provider_ref ON payments(provider, provider_reference_id);

-- 4. Append-Only Financial Ledger (WalletLedgerEntry)
CREATE TABLE IF NOT EXISTS wallet_ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT, -- NULL if platform account
    type VARCHAR(40) NOT NULL, -- CONSULTATION_PAYMENT, PLATFORM_COMMISSION, EXPERT_EARNING, REFUND, ADJUSTMENT, WITHDRAWAL
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'NPR',
    reference_type VARCHAR(50), -- PAYMENT, CONSULTATION, WITHDRAWAL
    reference_id BIGINT,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wle_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_wle_user_type ON wallet_ledger_entries(user_id, type);
CREATE INDEX IF NOT EXISTS idx_wle_ref ON wallet_ledger_entries(reference_type, reference_id);

-- 5. Withdrawal Requests table for Expert Earnings Payout
CREATE TABLE IF NOT EXISTS withdrawal_requests (
    id BIGSERIAL PRIMARY KEY,
    expert_id BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'NPR',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    account_details TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processed_by BIGINT,
    admin_notes VARCHAR(500),
    CONSTRAINT fk_wr_expert FOREIGN KEY (expert_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_wr_admin FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_wr_expert_status ON withdrawal_requests(expert_id, status);
