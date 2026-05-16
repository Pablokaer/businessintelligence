-- ============================================================
-- NexusFlow V3 — Sistema de níveis de acesso
-- ============================================================

-- Nova coluna access_level na tabela users
ALTER TABLE users
    ADD COLUMN access_level VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

-- O gerente existente vira OWNER
UPDATE users SET access_level = 'OWNER'    WHERE role = 'MANAGER';

-- Funcionários existentes ficam em STANDARD
UPDATE users SET access_level = 'STANDARD' WHERE role = 'EMPLOYEE';

-- Índice para queries por nível
CREATE INDEX idx_users_access_level ON users(access_level);

-- Promover Pedro a SUBMANAGER (exemplo de seed)
UPDATE users SET access_level = 'SUBMANAGER'
WHERE email = 'pedro@nexusflow.com';

-- Promover Julia a SENIOR (exemplo de seed)
UPDATE users SET access_level = 'SENIOR'
WHERE email = 'julia@nexusflow.com';
