-- ==========================================
-- Seed: Permissions and Default Roles
-- ==========================================

-- AUTH & ADMIN PERMISSIONS
INSERT INTO permission (code, description) VALUES
('USER_VIEW', 'View users'),
('USER_CREATE', 'Create users'),
('USER_EDIT', 'Edit users'),
('USER_DELETE', 'Delete users'),
('USER_ASSIGN_ROLE', 'Assign roles to users'),
('ROLE_VIEW', 'View roles'),
('ROLE_CREATE', 'Create roles'),
('ROLE_EDIT', 'Edit roles'),
('ROLE_DELETE', 'Delete roles'),
('ROLE_PERMISSION_MAP', 'Map permissions to roles'),
('TENANT_VIEW', 'View tenants'),
('TENANT_CREATE', 'Create tenants'),
('TENANT_EDIT', 'Edit tenants'),
('BRANCH_VIEW', 'View branches'),
('BRANCH_CREATE', 'Create branches'),
('BRANCH_EDIT', 'Edit branches'),
('SYSTEM_HEALTH_VIEW', 'View system health'),
('SYSTEM_LOG_VIEW', 'View system logs'),
('SYSTEM_ALERT_VIEW', 'View system alerts')
ON CONFLICT (code) DO NOTHING;

-- CHEMICAL MODULE
INSERT INTO permission (code, description) VALUES
('CHEMICAL_MASTER_VIEW', 'View chemical master'),
('CHEMICAL_MASTER_CREATE', 'Create chemical master'),
('CHEMICAL_MASTER_EDIT', 'Edit chemical master'),
('CHEMICAL_MASTER_DELETE', 'Delete chemical master'),
('CHEMICAL_REGISTER', 'Register chemicals'),
('CHEMICAL_VIEW', 'View chemicals'),
('CHEMICAL_EDIT', 'Edit chemicals'),
('CHEMICAL_STOCK_VIEW', 'View chemical stock'),
('CHEMICAL_STOCK_ADJUST', 'Adjust chemical stock'),
('CHEMICAL_MOVEMENT_VIEW', 'View chemical movements'),
('CHEMICAL_ISSUE', 'Issue chemicals'),
('CHEMICAL_DESTROY', 'Destroy chemicals'),
('CHEMICAL_RETIRE', 'Retire chemicals'),
('CHEMICAL_LABEL_PRINT', 'Print chemical labels'),
('CHEMICAL_EXPIRY_ALERT_VIEW', 'View expiry alerts'),
('CHEMICAL_REORDER_ALERT_VIEW', 'View reorder alerts')
ON CONFLICT (code) DO NOTHING;

-- INSTRUMENT MODULE
INSERT INTO permission (code, description) VALUES
('INSTRUMENT_VIEW', 'View instruments'),
('INSTRUMENT_CREATE', 'Create instruments'),
('INSTRUMENT_EDIT', 'Edit instruments'),
('INSTRUMENT_DEACTIVATE', 'Deactivate instruments'),
('CALIBRATION_SCHEDULE_VIEW', 'View calibration schedule'),
('CALIBRATION_SCHEDULE_EDIT', 'Edit calibration schedule'),
('CALIBRATION_ALLOCATE', 'Allocate calibration'),
('CALIBRATION_EXECUTE', 'Execute calibration'),
('CALIBRATION_REVIEW', 'Review calibration'),
('CALIBRATION_APPROVE', 'Approve calibration'),
('CALIBRATION_TREND_VIEW', 'View calibration trends'),
('MAINTENANCE_VIEW', 'View maintenance'),
('MAINTENANCE_CREATE', 'Create maintenance'),
('MAINTENANCE_APPROVE', 'Approve maintenance'),
('DOWNTIME_LOG', 'Log instrument downtime'),
('DOWNTIME_VIEW', 'View downtime'),
('QUALIFICATION_VIEW', 'View qualification'),
('QUALIFICATION_EXECUTE', 'Execute qualification'),
('QUALIFICATION_APPROVE', 'Approve qualification')
ON CONFLICT (code) DO NOTHING;

-- INVENTORY MODULE
INSERT INTO permission (code, description) VALUES
('INVENTORY_ITEM_VIEW', 'View inventory items'),
('INVENTORY_ITEM_CREATE', 'Create inventory items'),
('INVENTORY_ITEM_EDIT', 'Edit inventory items'),
('INVENTORY_STOCK_VIEW', 'View inventory stock'),
('INVENTORY_STOCK_ADJUST', 'Adjust inventory stock'),
('INVENTORY_EXPIRY_ALERT_VIEW', 'View inventory expiry alerts'),
('INVENTORY_REORDER_ALERT_VIEW', 'View inventory reorder alerts')
ON CONFLICT (code) DO NOTHING;

-- SUPPLIER MODULE
INSERT INTO permission (code, description) VALUES
('SUPPLIER_VIEW', 'View suppliers'),
('SUPPLIER_CREATE', 'Create suppliers'),
('SUPPLIER_EDIT', 'Edit suppliers'),
('SUPPLIER_DOCUMENT_UPLOAD', 'Upload supplier documents'),
('SUPPLIER_DOCUMENT_VIEW', 'View supplier documents'),
('SUPPLIER_RATING', 'Rate suppliers'),
('SUPPLIER_ITEM_MAP', 'Map items to suppliers')
ON CONFLICT (code) DO NOTHING;

-- OMS MODULE
INSERT INTO permission (code, description) VALUES
('ORDER_VIEW', 'View orders'),
('ORDER_CREATE', 'Create orders'),
('ORDER_EDIT', 'Edit orders'),
('ORDER_APPROVE', 'Approve orders'),
('ORDER_CANCEL', 'Cancel orders'),
('PO_VIEW', 'View purchase orders'),
('PO_CREATE', 'Create purchase orders'),
('PO_APPROVE', 'Approve purchase orders'),
('PO_PRINT', 'Print purchase orders'),
('GRN_VIEW', 'View goods receipts'),
('GRN_CREATE', 'Create goods receipts'),
('GRN_APPROVE', 'Approve goods receipts')
ON CONFLICT (code) DO NOTHING;

-- QA/QC MODULE
INSERT INTO permission (code, description) VALUES
('DEVIATION_VIEW', 'View deviations'),
('DEVIATION_CREATE', 'Create deviations'),
('DEVIATION_INVESTIGATE', 'Investigate deviations'),
('DEVIATION_CLOSE', 'Close deviations'),
('OOS_VIEW', 'View OOS cases'),
('OOS_CREATE', 'Create OOS cases'),
('OOS_INVESTIGATE', 'Investigate OOS cases'),
('OOS_APPROVE', 'Approve OOS cases'),
('CAPA_VIEW', 'View CAPA'),
('CAPA_CREATE', 'Create CAPA'),
('CAPA_ASSIGN', 'Assign CAPA'),
('CAPA_CLOSE', 'Close CAPA'),
('AUDIT_VIEW', 'View audit trail')
ON CONFLICT (code) DO NOTHING;

-- SAMPLE & TEST MODULE
INSERT INTO permission (code, description) VALUES
('SAMPLE_REGISTER', 'Register samples'),
('SAMPLE_VIEW', 'View samples'),
('SAMPLE_EDIT', 'Edit samples'),
('TEST_ASSIGN', 'Assign tests'),
('TEST_EXECUTE', 'Execute tests'),
('TEST_REVIEW', 'Review tests'),
('TEST_APPROVE', 'Approve tests'),
('RESULT_ENTER', 'Enter test results'),
('RESULT_REVIEW', 'Review test results'),
('RESULT_APPROVE', 'Approve test results'),
('COA_GENERATE', 'Generate COA'),
('COA_APPROVE', 'Approve COA'),
('COA_PRINT', 'Print COA')
ON CONFLICT (code) DO NOTHING;

-- AI MODULE
INSERT INTO permission (code, description) VALUES
('AI_INVENTORY_FORECAST_VIEW', 'View AI inventory forecasts'),
('AI_OOS_RISK_VIEW', 'View AI OOS risk'),
('AI_INSTRUMENT_TREND_VIEW', 'View AI instrument trends'),
('AI_WORKLOAD_VIEW', 'View AI workload predictions'),
('AI_AUTO_ORDER_INITIATE', 'Initiate AI auto orders')
ON CONFLICT (code) DO NOTHING;

-- DASHBOARD WIDGETS
INSERT INTO permission (code, description) VALUES
('WIDGET_CRITICAL_ALERTS', 'Widget: Critical alerts'),
('WIDGET_WORKLOAD', 'Widget: Workload'),
('WIDGET_LOW_STOCK', 'Widget: Low stock'),
('WIDGET_CALIBRATION_DUE', 'Widget: Calibration due'),
('WIDGET_OOS', 'Widget: OOS'),
('WIDGET_EXECUTIVE_KPI', 'Widget: Executive KPI'),
('WIDGET_AI_INSIGHTS', 'Widget: AI insights')
ON CONFLICT (code) DO NOTHING;

-- Default Roles
INSERT INTO role (code, name, description) VALUES
('SUPER_ADMIN', 'Super Administrator', 'Full system access'),
('LAB_MANAGER', 'Lab Manager', 'Lab management and approvals'),
('ANALYST', 'Analyst', 'Test execution and result entry'),
('REVIEWER', 'Reviewer', 'Result review and approval'),
('APPROVER', 'Approver', 'Final approval authority'),
('INVENTORY_MANAGER', 'Inventory Manager', 'Chemical and inventory management'),
('INSTRUMENT_MANAGER', 'Instrument Manager', 'Instrument and calibration management'),
('QA_MANAGER', 'QA Manager', 'Quality assurance management'),
('PURCHASER', 'Purchaser', 'Order and purchase management'),
('VIEWER', 'Read-Only Viewer', 'Read-only access to all modules')
ON CONFLICT (code) DO NOTHING;
