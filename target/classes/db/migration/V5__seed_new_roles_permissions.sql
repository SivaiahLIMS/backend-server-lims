/*
  # V5 - New Roles and Permissions Seed

  Adds the roles and permissions defined in the LIMS design document v2:

  New Roles:
    - ANALYST, STOREKEEPER, QA_MANAGER, QC_MANAGER, LAB_MANAGER, AI_REVIEWER

  New Permissions for modules added in V4:
    - Storage, ELN, Training, Task Engine, Barcode, Calibration Limits, Analytics

  Note: ADMIN and base roles (SUPER_ADMIN, etc.) already exist from V2.
*/

-- New role-level permissions for new modules
INSERT INTO permission (code, description) VALUES
('STORAGE_VIEW', 'View storage locations'),
('STORAGE_CREATE', 'Create storage locations'),
('STORAGE_PLACE_CONTAINER', 'Place container in storage'),
('STORAGE_MOVE_CONTAINER', 'Move container between locations'),
('STORAGE_VIOLATION_VIEW', 'View storage violations'),
('STORAGE_VIOLATION_RESOLVE', 'Resolve storage violations'),
('ELN_VIEW', 'View ELN entries'),
('ELN_CREATE', 'Create ELN entries'),
('ELN_EDIT', 'Edit own ELN entries'),
('TRAINING_VIEW', 'View training materials'),
('TRAINING_CREATE', 'Create training material'),
('TRAINING_ASSIGN', 'Assign training to users'),
('TRAINING_COMPLETE', 'Mark training as completed'),
('TRAINING_APPROVE', 'Approve training completion'),
('TASK_VIEW', 'View tasks'),
('TASK_CREATE', 'Create tasks'),
('TASK_ACCEPT', 'Accept assigned tasks'),
('TASK_COMPLETE', 'Complete tasks'),
('TASK_APPROVE', 'Approve/reject tasks'),
('BARCODE_SCAN', 'Perform barcode scans'),
('CONTAINER_VIEW', 'View chemical containers'),
('CONTAINER_CREATE', 'Create chemical containers'),
('CONTAINER_RESERVE', 'Reserve chemical containers'),
('CONTAINER_CONSUME', 'Consume/convert reservations'),
('INSTRUMENT_RESERVE', 'Reserve instruments'),
('INSTRUMENT_RESERVATION_APPROVE', 'Approve instrument reservations'),
('CALIBRATION_LIMIT_VIEW', 'View calibration limit sets'),
('CALIBRATION_LIMIT_CREATE', 'Create calibration limit sets'),
('CALIBRATION_TASK_VIEW', 'View calibration tasks'),
('CALIBRATION_TASK_CREATE', 'Create calibration tasks'),
('CALIBRATION_TASK_COMPLETE', 'Complete calibration tasks'),
('OOS_VIEW', 'View OOS/OOT test results'),
('OOS_INVESTIGATE', 'Initiate OOS investigation'),
('OOS_APPROVE', 'Approve OOS investigation'),
('ANALYTICS_VIEW', 'View analytics dashboards'),
('PREDICTIVE_ALERT_VIEW', 'View predictive alerts'),
('PREDICTIVE_ALERT_ACK', 'Acknowledge predictive alerts'),
('USER_SKILL_VIEW', 'View user skills'),
('USER_SKILL_EDIT', 'Edit user skills'),
('USER_WORKLOAD_VIEW', 'View user workload')
ON CONFLICT (code) DO NOTHING;

-- Insert new design-doc roles
INSERT INTO role (code, name, description) VALUES
('ANALYST', 'Analyst', 'Lab analyst: execute worksheets, ELN, OOS investigation'),
('STOREKEEPER', 'Storekeeper', 'Receive chemicals, move containers, manage storage'),
('QA_MANAGER', 'QA Manager', 'Approve worksheets, templates, OOS; manage QA domain'),
('QC_MANAGER', 'QC Manager', 'Same as QA but for QC domain'),
('LAB_MANAGER', 'Lab Manager', 'Approve instrument reservations, create calibration tasks, analytics'),
('AI_REVIEWER', 'AI Reviewer', 'System identity for automated approvals and predictions')
ON CONFLICT (code) DO NOTHING;
