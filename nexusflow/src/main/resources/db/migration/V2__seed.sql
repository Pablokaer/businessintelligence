-- ============================================================
-- NexusFlow V2 — Dados iniciais de exemplo
-- Senhas: manager123 / emp123  (BCrypt cost 12)
-- ============================================================

INSERT INTO users (id, name, email, password_hash, role) VALUES
('a0000000-0000-0000-0000-000000000001','Carlos Souza','carlos@nexusflow.com',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/HS.iK1O','MANAGER');

INSERT INTO users (id, name, email, password_hash, role, manager_id) VALUES
('b0000000-0000-0000-0000-000000000001','Ana Lima','ana@nexusflow.com',
 '$2a$12$sGpBiUZVk7fM7D4nX/nFq.1fYHEJtKJLR9o7u3l9Nx5fXYAQpnOYi','EMPLOYEE',
 'a0000000-0000-0000-0000-000000000001'),
('b0000000-0000-0000-0000-000000000002','Pedro Rocha','pedro@nexusflow.com',
 '$2a$12$sGpBiUZVk7fM7D4nX/nFq.1fYHEJtKJLR9o7u3l9Nx5fXYAQpnOYi','EMPLOYEE',
 'a0000000-0000-0000-0000-000000000001'),
('b0000000-0000-0000-0000-000000000003','Julia Martins','julia@nexusflow.com',
 '$2a$12$sGpBiUZVk7fM7D4nX/nFq.1fYHEJtKJLR9o7u3l9Nx5fXYAQpnOYi','EMPLOYEE',
 'a0000000-0000-0000-0000-000000000001');

INSERT INTO submissions
  (user_id,type,status,value,hours,form_number,description,category,satisfaction,occurrence_date,reviewed_by,reviewed_at)
VALUES
  ('b0000000-0000-0000-0000-000000000001','SALE','APPROVED',450.00,2.5,'F-2026-00001','Venda balcão','Varejo',5,CURRENT_DATE-2,'a0000000-0000-0000-0000-000000000001',NOW()),
  ('b0000000-0000-0000-0000-000000000002','EXPENSE','PENDING',120.50,1.0,'F-2026-00002','Material limpeza','Operacional',3,CURRENT_DATE-2,NULL,NULL),
  ('b0000000-0000-0000-0000-000000000001','SERVICE','APPROVED',800.00,4.0,'F-2026-00003','Manutenção AC','Manutenção',4,CURRENT_DATE-1,'a0000000-0000-0000-0000-000000000001',NOW()),
  ('b0000000-0000-0000-0000-000000000003','SALE','APPROVED',1200.00,3.0,'F-2026-00004','Venda atacado','Varejo',5,CURRENT_DATE-1,'a0000000-0000-0000-0000-000000000001',NOW()),
  ('b0000000-0000-0000-0000-000000000002','REFUND','PENDING',75.00,0.5,'F-2026-00005','Devolução cliente','Varejo',2,CURRENT_DATE,NULL,NULL),
  ('b0000000-0000-0000-0000-000000000003','SALE','APPROVED',340.00,1.5,'F-2026-00006','Venda online','E-commerce',5,CURRENT_DATE,'a0000000-0000-0000-0000-000000000001',NOW());

SELECT setval('form_seq', 6);
