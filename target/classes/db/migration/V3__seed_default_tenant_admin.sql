-- ==========================================
-- Seed: Default Tenant and Admin User
-- ==========================================

-- Default tenant
INSERT INTO tenant (name, code, status) VALUES
('Siva Ya Health', 'SIVAYA', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- Default branch
INSERT INTO branch (tenant_id, name, code, status)
SELECT id, 'Main Branch', 'MAIN', 'ACTIVE' FROM tenant WHERE code = 'SIVAYA'
ON CONFLICT DO NOTHING;

-- Default admin user (password: Admin@123 - BCrypt)
INSERT INTO app_user (tenant_id, username, password_hash, email, status)
SELECT id, 'admin',
       '$2a$12$rBXfnMC8WW5jcnXf3Q3bqe4o5KY5fYmWlp0bFN59u2FIe7BBPS5Wm',
       'admin@sivayahealth.com', 'ACTIVE'
FROM tenant WHERE code = 'SIVAYA'
ON CONFLICT DO NOTHING;

-- Admin profile
INSERT INTO user_profile (user_id, first_name, last_name)
SELECT u.id, 'System', 'Administrator'
FROM app_user u
JOIN tenant t ON t.id = u.tenant_id
WHERE u.username = 'admin' AND t.code = 'SIVAYA'
ON CONFLICT DO NOTHING;

-- Assign SUPER_ADMIN role to admin
INSERT INTO user_role (user_id, tenant_id, branch_id, role_id)
SELECT u.id, t.id, b.id, r.id
FROM app_user u
JOIN tenant t ON t.id = u.tenant_id
JOIN branch b ON b.tenant_id = t.id AND b.code = 'MAIN'
JOIN role r ON r.code = 'SUPER_ADMIN'
WHERE u.username = 'admin' AND t.code = 'SIVAYA'
ON CONFLICT DO NOTHING;

-- Grant all permissions to SUPER_ADMIN role for SIVAYA tenant
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- Grant ANALYST permissions
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'ANALYST'
  AND p.code IN (
    'SAMPLE_VIEW', 'SAMPLE_REGISTER', 'TEST_EXECUTE', 'RESULT_ENTER',
    'CHEMICAL_VIEW', 'CHEMICAL_STOCK_VIEW', 'CHEMICAL_ISSUE',
    'INSTRUMENT_VIEW', 'CALIBRATION_EXECUTE', 'DOWNTIME_LOG',
    'WIDGET_WORKLOAD', 'WIDGET_CALIBRATION_DUE'
  )
ON CONFLICT DO NOTHING;

-- Grant LAB_MANAGER permissions
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'LAB_MANAGER'
  AND p.code IN (
    'USER_VIEW', 'SAMPLE_VIEW', 'SAMPLE_REGISTER', 'SAMPLE_EDIT',
    'TEST_ASSIGN', 'TEST_EXECUTE', 'TEST_REVIEW', 'TEST_APPROVE',
    'RESULT_ENTER', 'RESULT_REVIEW', 'RESULT_APPROVE',
    'COA_GENERATE', 'COA_APPROVE', 'COA_PRINT',
    'CHEMICAL_VIEW', 'CHEMICAL_REGISTER', 'CHEMICAL_STOCK_VIEW', 'CHEMICAL_ISSUE',
    'INSTRUMENT_VIEW', 'CALIBRATION_ALLOCATE', 'CALIBRATION_APPROVE',
    'DEVIATION_VIEW', 'DEVIATION_CREATE', 'DEVIATION_CLOSE',
    'OOS_VIEW', 'OOS_CREATE', 'OOS_APPROVE',
    'CAPA_VIEW', 'CAPA_CREATE', 'CAPA_ASSIGN', 'CAPA_CLOSE',
    'WIDGET_CRITICAL_ALERTS', 'WIDGET_WORKLOAD', 'WIDGET_EXECUTIVE_KPI',
    'WIDGET_LOW_STOCK', 'WIDGET_CALIBRATION_DUE', 'WIDGET_OOS',
    'SYSTEM_LOG_VIEW', 'AUDIT_VIEW'
  )
ON CONFLICT DO NOTHING;
