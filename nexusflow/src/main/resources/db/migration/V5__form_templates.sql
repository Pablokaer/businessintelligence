-- ============================================================
-- NexusFlow V5 — Sistema de formulários dinâmicos
-- ============================================================

-- Tabela de templates de formulário (criados pelos managers)
CREATE TABLE form_templates (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id     UUID         NOT NULL REFERENCES users(id),
    name         VARCHAR(120) NOT NULL,
    description  VARCHAR(500),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Campos customizados de cada template
CREATE TABLE form_fields (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id   UUID         NOT NULL REFERENCES form_templates(id) ON DELETE CASCADE,
    label         VARCHAR(120) NOT NULL,
    field_type    VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    required      BOOLEAN      NOT NULL DEFAULT FALSE,
    options       TEXT,        -- valores separados por vírgula para tipo SELECT
    display_order INT          NOT NULL DEFAULT 0
);

-- Submissões preenchidas pelos funcionários
CREATE TABLE form_submissions (
    id              UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id     UUID              NOT NULL REFERENCES form_templates(id),
    submitted_by    UUID              NOT NULL REFERENCES users(id),
    status          submission_status NOT NULL DEFAULT 'PENDING',
    service_cost    NUMERIC(12,2)     NOT NULL DEFAULT 0,
    service_hours   NUMERIC(5,2)      NOT NULL DEFAULT 0,
    service_value   NUMERIC(12,2)     NOT NULL DEFAULT 0,
    occurrence_date DATE              NOT NULL DEFAULT CURRENT_DATE,
    notes           TEXT,
    reviewed_by     UUID              REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);

-- Respostas dos campos customizados
CREATE TABLE form_field_responses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES form_submissions(id) ON DELETE CASCADE,
    field_id      UUID NOT NULL REFERENCES form_fields(id),
    response      TEXT
);

CREATE INDEX idx_form_templates_owner      ON form_templates(owner_id);
CREATE INDEX idx_form_fields_template      ON form_fields(template_id);
CREATE INDEX idx_form_submissions_template ON form_submissions(template_id);
CREATE INDEX idx_form_submissions_user     ON form_submissions(submitted_by);
CREATE INDEX idx_form_submissions_status   ON form_submissions(status);
CREATE INDEX idx_form_submissions_date     ON form_submissions(occurrence_date);

CREATE TRIGGER trg_form_templates_upd BEFORE UPDATE ON form_templates
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_form_submissions_upd BEFORE UPDATE ON form_submissions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
