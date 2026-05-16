-- ============================================================
-- NexusFlow V1 — Schema inicial
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE user_role        AS ENUM ('MANAGER','EMPLOYEE');
CREATE TYPE submission_type  AS ENUM ('SALE','EXPENSE','SERVICE','REFUND');
CREATE TYPE submission_status AS ENUM ('PENDING','APPROVED','REJECTED');

-- ── USERS ────────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120)  NOT NULL,
    email         VARCHAR(255)  NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    role          user_role     NOT NULL DEFAULT 'EMPLOYEE',
    manager_id    UUID          REFERENCES users(id) ON DELETE SET NULL,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email   ON users(email);
CREATE INDEX idx_users_manager ON users(manager_id);

-- ── SUBMISSIONS ──────────────────────────────────────────────
CREATE TABLE submissions (
    id              UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID               NOT NULL REFERENCES users(id),
    type            submission_type    NOT NULL,
    status          submission_status  NOT NULL DEFAULT 'PENDING',
    value           NUMERIC(12,2)      NOT NULL CHECK (value >= 0),
    hours           NUMERIC(5,2)       NOT NULL CHECK (hours >= 0),
    form_number     VARCHAR(30)        NOT NULL UNIQUE,
    description     VARCHAR(500)       NOT NULL,
    category        VARCHAR(100)       NOT NULL,
    satisfaction    SMALLINT           CHECK (satisfaction BETWEEN 1 AND 5),
    notes           TEXT,
    occurrence_date DATE               NOT NULL,
    reviewed_by     UUID               REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ        NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sub_user     ON submissions(user_id);
CREATE INDEX idx_sub_status   ON submissions(status);
CREATE INDEX idx_sub_type     ON submissions(type);
CREATE INDEX idx_sub_date     ON submissions(occurrence_date);
CREATE INDEX idx_sub_category ON submissions(category);

-- ── TRIGGER updated_at ───────────────────────────────────────
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_upd BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_sub_upd BEFORE UPDATE ON submissions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── SEQUENCE para form_number ────────────────────────────────
CREATE SEQUENCE form_seq START 1 INCREMENT 1;
