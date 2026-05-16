-- ============================================================
-- NexusFlow V4 — Role SUPER_ADMIN
-- Nota: ALTER TYPE ADD VALUE foi executado fora do Flyway pois
-- PostgreSQL não permite esse DDL dentro de bloco de transação.
-- ============================================================

-- Conta super-admin de seed (senha: super123)
INSERT INTO users (name, email, password_hash, role, access_level)
VALUES (
    'Super Admin',
    'superadmin@nexusflow.com',
    crypt('super123', gen_salt('bf', 12)),
    'SUPER_ADMIN',
    'PLATFORM'
);
