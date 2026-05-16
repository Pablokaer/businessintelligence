-- Adiciona status DRAFT ao enum submission_status
ALTER TYPE submission_status ADD VALUE IF NOT EXISTS 'DRAFT';

-- Coluna de token de compartilhamento (única por rascunho)
ALTER TABLE form_submissions
    ADD COLUMN IF NOT EXISTS share_token UUID UNIQUE;

CREATE INDEX IF NOT EXISTS idx_form_submissions_share_token
    ON form_submissions (share_token);
