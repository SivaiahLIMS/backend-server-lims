/*
  # V7 - Seed Users for Every Role

  Creates one named test/demo user per role so every UI screen can be
  accessed without needing to call the API manually.

  ## Users (all under SIVAYA tenant, MAIN branch)

  | Username      | Password  | Role               | Module Focus                               |
  |---------------|-----------|--------------------|--------------------------------------------|
  | admin         | Admin@123 | SUPER_ADMIN        | Already exists from V3 - skipped here      |
  | lab_manager   | Lims@1234 | LAB_MANAGER        | Full lab ops, approvals, executive views   |
  | analyst       | Lims@1234 | ANALYST            | Test execution, result entry, ELN, samples |
  | reviewer      | Lims@1234 | REVIEWER           | Result/COA review, QA read access          |
  | approver      | Lims@1234 | APPROVER           | Final approvals across all modules         |
  | inv_manager   | Lims@1234 | INVENTORY_MANAGER  | Chemicals, containers, storage, OMS        |
  | inst_manager  | Lims@1234 | INSTRUMENT_MANAGER | Instruments, calibration, maintenance      |
  | qa_manager    | Lims@1234 | QA_MANAGER         | QA domain: deviations, OOS, CAPA           |
  | qc_manager    | Lims@1234 | QC_MANAGER         | QC domain: samples, COA, OOS               |
  | purchaser     | Lims@1234 | PURCHASER          | Orders, POs, GRNs, supplier management     |
  | storekeeper   | Lims@1234 | STOREKEEPER        | Receiving, containers, storage movement    |
  | ai_reviewer   | Lims@1234 | AI_REVIEWER        | AI predictions, analytics, predictive alerts|
  | viewer        | Lims@1234 | VIEWER             | Read-only across all modules               |

  ## Password Hashes
  - Admin@123  → $2a$12$rBXfnMC8WW5jcnXf3Q3bqe4o5KY5fYmWlp0bFN59u2FIe7BBPS5Wm  (from V3)
  - Lims@1234  → $2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh3y

  ## Notes
  1. Roles REVIEWER, APPROVER, INVENTORY_MANAGER, INSTRUMENT_MANAGER, PURCHASER,
     STOREKEEPER, QC_MANAGER, AI_REVIEWER, and VIEWER had no permission grants in V3/V5.
     This migration grants them full, sensible permissions.
  2. LAB_MANAGER and ANALYST were already granted in V3; those grants are left untouched.
  3. All inserts use ON CONFLICT DO NOTHING — safe to re-run.
*/

-- ============================================================
-- SECTION 1: Users
-- ============================================================
INSERT INTO app_user (tenant_id, username, password_hash, email, status)
SELECT t.id,
       u.username,
       '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh3y',
       u.email,
       'ACTIVE'
FROM tenant t,
     (VALUES
          ('lab_manager',  'lab.manager@sivayahealth.com'),
          ('analyst',      'analyst@sivayahealth.com'),
          ('reviewer',     'reviewer@sivayahealth.com'),
          ('approver',     'approver@sivayahealth.com'),
          ('inv_manager',  'inv.manager@sivayahealth.com'),
          ('inst_manager', 'inst.manager@sivayahealth.com'),
          ('qa_manager',   'qa.manager@sivayahealth.com'),
          ('qc_manager',   'qc.manager@sivayahealth.com'),
          ('purchaser',    'purchaser@sivayahealth.com'),
          ('storekeeper',  'storekeeper@sivayahealth.com'),
          ('ai_reviewer',  'ai.reviewer@sivayahealth.com'),
          ('viewer',       'viewer@sivayahealth.com')
     ) AS u(username, email)
WHERE t.code = 'SIVAYA'
    ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- SECTION 2: User Profiles
-- ============================================================
INSERT INTO user_profile (user_id, first_name, last_name)
SELECT au.id, names.first_name, names.last_name
FROM app_user au
         JOIN tenant t ON t.id = au.tenant_id AND t.code = 'SIVAYA',
     (VALUES
          ('lab_manager',  'Lab',        'Manager'),
          ('analyst',      'Lab',        'Analyst'),
          ('reviewer',     'Result',     'Reviewer'),
          ('approver',     'Final',      'Approver'),
          ('inv_manager',  'Inventory',  'Manager'),
          ('inst_manager', 'Instrument', 'Manager'),
          ('qa_manager',   'QA',         'Manager'),
          ('qc_manager',   'QC',         'Manager'),
          ('purchaser',    'Purchase',   'Officer'),
          ('storekeeper',  'Store',      'Keeper'),
          ('ai_reviewer',  'AI',         'Reviewer'),
          ('viewer',       'Read',       'Only')
     ) AS names(username, first_name, last_name)
WHERE au.username = names.username
ON CONFLICT (user_id) DO NOTHING;

-- ============================================================
-- SECTION 3: Role Assignments
-- ============================================================
INSERT INTO user_role (user_id, tenant_id, branch_id, role_id)
SELECT au.id, t.id, b.id, r.id
FROM app_user au
         JOIN tenant t ON t.id = au.tenant_id AND t.code = 'SIVAYA'
         JOIN branch b ON b.tenant_id = t.id AND b.code = 'MAIN'
         JOIN role r ON r.code = ra.role_code,
     (VALUES
          ('lab_manager',  'LAB_MANAGER'),
          ('analyst',      'ANALYST'),
          ('reviewer',     'REVIEWER'),
          ('approver',     'APPROVER'),
          ('inv_manager',  'INVENTORY_MANAGER'),
          ('inst_manager', 'INSTRUMENT_MANAGER'),
          ('qa_manager',   'QA_MANAGER'),
          ('qc_manager',   'QC_MANAGER'),
          ('purchaser',    'PURCHASER'),
          ('storekeeper',  'STOREKEEPER'),
          ('ai_reviewer',  'AI_REVIEWER'),
          ('viewer',       'VIEWER')
     ) AS ra(username, role_code)
WHERE au.username = ra.username
    ON CONFLICT DO NOTHING;

-- ============================================================
-- SECTION 4: Permission Grants per Role
-- ============================================================

-- REVIEWER: review results, COA, read QA/QC modules
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'REVIEWER'
  AND p.code IN (
                 'SAMPLE_VIEW',
                 'TEST_REVIEW', 'RESULT_REVIEW', 'RESULT_APPROVE',
                 'COA_GENERATE', 'COA_PRINT',
                 'CHEMICAL_VIEW', 'CHEMICAL_STOCK_VIEW',
                 'INSTRUMENT_VIEW', 'CALIBRATION_SCHEDULE_VIEW', 'CALIBRATION_TREND_VIEW',
                 'DEVIATION_VIEW', 'OOS_VIEW', 'CAPA_VIEW',
                 'ELN_VIEW', 'ANALYTICS_VIEW',
                 'AUDIT_VIEW',
                 'WIDGET_OOS', 'WIDGET_CALIBRATION_DUE', 'WIDGET_WORKLOAD'
    )
    ON CONFLICT DO NOTHING;

-- APPROVER: final approval across all modules
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'APPROVER'
  AND p.code IN (
                 'SAMPLE_VIEW',
                 'TEST_APPROVE', 'RESULT_REVIEW', 'RESULT_APPROVE',
                 'COA_GENERATE', 'COA_APPROVE', 'COA_PRINT',
                 'CHEMICAL_VIEW', 'CHEMICAL_STOCK_VIEW', 'CHEMICAL_DESTROY',
                 'INSTRUMENT_VIEW', 'CALIBRATION_APPROVE', 'QUALIFICATION_APPROVE',
                 'MAINTENANCE_APPROVE',
                 'DEVIATION_VIEW', 'DEVIATION_CLOSE',
                 'OOS_VIEW', 'OOS_APPROVE',
                 'CAPA_VIEW', 'CAPA_CLOSE',
                 'TASK_APPROVE',
                 'INSTRUMENT_RESERVATION_APPROVE',
                 'ELN_VIEW', 'ANALYTICS_VIEW',
                 'AUDIT_VIEW',
                 'WIDGET_CRITICAL_ALERTS', 'WIDGET_OOS', 'WIDGET_EXECUTIVE_KPI'
    )
    ON CONFLICT DO NOTHING;

-- INVENTORY_MANAGER: full chemical, inventory, container, storage, limited OMS
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'INVENTORY_MANAGER'
  AND p.code IN (
                 'CHEMICAL_MASTER_VIEW', 'CHEMICAL_MASTER_CREATE', 'CHEMICAL_MASTER_EDIT',
                 'CHEMICAL_REGISTER', 'CHEMICAL_VIEW', 'CHEMICAL_EDIT',
                 'CHEMICAL_STOCK_VIEW', 'CHEMICAL_STOCK_ADJUST',
                 'CHEMICAL_MOVEMENT_VIEW', 'CHEMICAL_ISSUE', 'CHEMICAL_DESTROY', 'CHEMICAL_RETIRE',
                 'CHEMICAL_LABEL_PRINT', 'CHEMICAL_EXPIRY_ALERT_VIEW', 'CHEMICAL_REORDER_ALERT_VIEW',
                 'INVENTORY_ITEM_VIEW', 'INVENTORY_ITEM_CREATE', 'INVENTORY_ITEM_EDIT',
                 'INVENTORY_STOCK_VIEW', 'INVENTORY_STOCK_ADJUST',
                 'INVENTORY_EXPIRY_ALERT_VIEW', 'INVENTORY_REORDER_ALERT_VIEW',
                 'CONTAINER_VIEW', 'CONTAINER_CREATE', 'CONTAINER_RESERVE', 'CONTAINER_CONSUME',
                 'STORAGE_VIEW', 'STORAGE_CREATE', 'STORAGE_PLACE_CONTAINER', 'STORAGE_MOVE_CONTAINER',
                 'STORAGE_VIOLATION_VIEW', 'STORAGE_VIOLATION_RESOLVE',
                 'SUPPLIER_VIEW', 'SUPPLIER_ITEM_MAP',
                 'ORDER_VIEW', 'PO_VIEW', 'GRN_VIEW', 'GRN_CREATE',
                 'BARCODE_SCAN',
                 'TASK_VIEW', 'TASK_CREATE', 'TASK_ACCEPT', 'TASK_COMPLETE',
                 'AI_INVENTORY_FORECAST_VIEW', 'PREDICTIVE_ALERT_VIEW', 'PREDICTIVE_ALERT_ACK',
                 'ANALYTICS_VIEW', 'AUDIT_VIEW',
                 'WIDGET_LOW_STOCK', 'WIDGET_CRITICAL_ALERTS'
    )
    ON CONFLICT DO NOTHING;

-- INSTRUMENT_MANAGER: full instrument, calibration, maintenance
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'INSTRUMENT_MANAGER'
  AND p.code IN (
                 'INSTRUMENT_VIEW', 'INSTRUMENT_CREATE', 'INSTRUMENT_EDIT', 'INSTRUMENT_DEACTIVATE',
                 'INSTRUMENT_RESERVE', 'INSTRUMENT_RESERVATION_APPROVE',
                 'CALIBRATION_SCHEDULE_VIEW', 'CALIBRATION_SCHEDULE_EDIT',
                 'CALIBRATION_ALLOCATE', 'CALIBRATION_EXECUTE', 'CALIBRATION_REVIEW', 'CALIBRATION_APPROVE',
                 'CALIBRATION_TREND_VIEW',
                 'CALIBRATION_LIMIT_VIEW', 'CALIBRATION_LIMIT_CREATE',
                 'CALIBRATION_TASK_VIEW', 'CALIBRATION_TASK_CREATE', 'CALIBRATION_TASK_COMPLETE',
                 'MAINTENANCE_VIEW', 'MAINTENANCE_CREATE', 'MAINTENANCE_APPROVE',
                 'DOWNTIME_LOG', 'DOWNTIME_VIEW',
                 'QUALIFICATION_VIEW', 'QUALIFICATION_EXECUTE', 'QUALIFICATION_APPROVE',
                 'BARCODE_SCAN',
                 'TASK_VIEW', 'TASK_CREATE', 'TASK_APPROVE',
                 'AI_INSTRUMENT_TREND_VIEW', 'PREDICTIVE_ALERT_VIEW', 'PREDICTIVE_ALERT_ACK',
                 'ANALYTICS_VIEW', 'AUDIT_VIEW',
                 'WIDGET_CALIBRATION_DUE', 'WIDGET_CRITICAL_ALERTS'
    )
    ON CONFLICT DO NOTHING;

-- PURCHASER: full OMS, supplier management
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'PURCHASER'
  AND p.code IN (
                 'ORDER_VIEW', 'ORDER_CREATE', 'ORDER_EDIT', 'ORDER_APPROVE', 'ORDER_CANCEL',
                 'PO_VIEW', 'PO_CREATE', 'PO_APPROVE', 'PO_PRINT',
                 'GRN_VIEW', 'GRN_CREATE', 'GRN_APPROVE',
                 'SUPPLIER_VIEW', 'SUPPLIER_CREATE', 'SUPPLIER_EDIT',
                 'SUPPLIER_DOCUMENT_UPLOAD', 'SUPPLIER_DOCUMENT_VIEW',
                 'SUPPLIER_RATING', 'SUPPLIER_ITEM_MAP',
                 'INVENTORY_ITEM_VIEW', 'INVENTORY_STOCK_VIEW',
                 'CHEMICAL_MASTER_VIEW', 'CHEMICAL_VIEW', 'CHEMICAL_STOCK_VIEW',
                 'CHEMICAL_REORDER_ALERT_VIEW', 'INVENTORY_REORDER_ALERT_VIEW',
                 'AUDIT_VIEW'
    )
    ON CONFLICT DO NOTHING;

-- STOREKEEPER: receiving, containers, storage movement
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'STOREKEEPER'
  AND p.code IN (
                 'CHEMICAL_MASTER_VIEW', 'CHEMICAL_VIEW', 'CHEMICAL_REGISTER',
                 'CHEMICAL_STOCK_VIEW', 'CHEMICAL_ISSUE', 'CHEMICAL_LABEL_PRINT',
                 'CHEMICAL_EXPIRY_ALERT_VIEW',
                 'CONTAINER_VIEW', 'CONTAINER_CREATE', 'CONTAINER_RESERVE', 'CONTAINER_CONSUME',
                 'STORAGE_VIEW', 'STORAGE_PLACE_CONTAINER', 'STORAGE_MOVE_CONTAINER',
                 'STORAGE_VIOLATION_VIEW',
                 'INVENTORY_ITEM_VIEW', 'INVENTORY_STOCK_VIEW',
                 'GRN_VIEW', 'GRN_CREATE',
                 'BARCODE_SCAN',
                 'TASK_VIEW', 'TASK_ACCEPT', 'TASK_COMPLETE',
                 'WIDGET_LOW_STOCK'
    )
    ON CONFLICT DO NOTHING;

-- QC_MANAGER: full QC domain — samples, COA, OOS, CAPA
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'QC_MANAGER'
  AND p.code IN (
                 'SAMPLE_REGISTER', 'SAMPLE_VIEW', 'SAMPLE_EDIT',
                 'TEST_ASSIGN', 'TEST_EXECUTE', 'TEST_REVIEW', 'TEST_APPROVE',
                 'RESULT_ENTER', 'RESULT_REVIEW', 'RESULT_APPROVE',
                 'COA_GENERATE', 'COA_APPROVE', 'COA_PRINT',
                 'CHEMICAL_VIEW', 'CHEMICAL_STOCK_VIEW', 'CHEMICAL_ISSUE',
                 'INSTRUMENT_VIEW', 'CALIBRATION_SCHEDULE_VIEW', 'CALIBRATION_TREND_VIEW',
                 'DEVIATION_VIEW', 'DEVIATION_CREATE', 'DEVIATION_INVESTIGATE', 'DEVIATION_CLOSE',
                 'OOS_VIEW', 'OOS_CREATE', 'OOS_INVESTIGATE', 'OOS_APPROVE',
                 'CAPA_VIEW', 'CAPA_CREATE', 'CAPA_ASSIGN', 'CAPA_CLOSE',
                 'ELN_VIEW', 'ELN_CREATE', 'ELN_EDIT',
                 'TASK_VIEW', 'TASK_CREATE', 'TASK_ACCEPT', 'TASK_COMPLETE',
                 'AI_OOS_RISK_VIEW', 'ANALYTICS_VIEW',
                 'AUDIT_VIEW',
                 'WIDGET_CRITICAL_ALERTS', 'WIDGET_OOS', 'WIDGET_CALIBRATION_DUE'
    )
    ON CONFLICT DO NOTHING;

-- AI_REVIEWER: AI/analytics read + predictive alert management
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'AI_REVIEWER'
  AND p.code IN (
                 'AI_INVENTORY_FORECAST_VIEW', 'AI_OOS_RISK_VIEW',
                 'AI_INSTRUMENT_TREND_VIEW', 'AI_WORKLOAD_VIEW', 'AI_AUTO_ORDER_INITIATE',
                 'PREDICTIVE_ALERT_VIEW', 'PREDICTIVE_ALERT_ACK',
                 'ANALYTICS_VIEW',
                 'SAMPLE_VIEW', 'TEST_REVIEW',
                 'CHEMICAL_STOCK_VIEW', 'INSTRUMENT_VIEW',
                 'AUDIT_VIEW',
                 'WIDGET_AI_INSIGHTS', 'WIDGET_EXECUTIVE_KPI'
    )
    ON CONFLICT DO NOTHING;

-- VIEWER: read-only across every module
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'VIEWER'
  AND p.code IN (
                 'USER_VIEW',
                 'CHEMICAL_MASTER_VIEW', 'CHEMICAL_VIEW', 'CHEMICAL_STOCK_VIEW',
                 'CHEMICAL_MOVEMENT_VIEW', 'CHEMICAL_EXPIRY_ALERT_VIEW',
                 'INVENTORY_ITEM_VIEW', 'INVENTORY_STOCK_VIEW', 'INVENTORY_EXPIRY_ALERT_VIEW',
                 'INSTRUMENT_VIEW', 'CALIBRATION_SCHEDULE_VIEW', 'CALIBRATION_TREND_VIEW',
                 'CALIBRATION_LIMIT_VIEW', 'CALIBRATION_TASK_VIEW',
                 'MAINTENANCE_VIEW', 'DOWNTIME_VIEW', 'QUALIFICATION_VIEW',
                 'SUPPLIER_VIEW', 'SUPPLIER_DOCUMENT_VIEW',
                 'ORDER_VIEW', 'PO_VIEW', 'GRN_VIEW',
                 'DEVIATION_VIEW', 'OOS_VIEW', 'CAPA_VIEW',
                 'SAMPLE_VIEW', 'RESULT_REVIEW', 'COA_PRINT',
                 'ELN_VIEW', 'TRAINING_VIEW',
                 'TASK_VIEW', 'STORAGE_VIEW', 'CONTAINER_VIEW',
                 'ANALYTICS_VIEW', 'AUDIT_VIEW',
                 'WIDGET_CRITICAL_ALERTS', 'WIDGET_LOW_STOCK',
                 'WIDGET_CALIBRATION_DUE', 'WIDGET_OOS', 'WIDGET_WORKLOAD'
    )
    ON CONFLICT DO NOTHING;
