-- ==========================================
-- LIMS Database Schema v1.0
-- ==========================================

-- Tenant & Branch
CREATE TABLE IF NOT EXISTS tenant (
  id           BIGSERIAL PRIMARY KEY,
  name         VARCHAR(200) NOT NULL,
  code         VARCHAR(50) UNIQUE,
  status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS branch (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  name         VARCHAR(200) NOT NULL,
  code         VARCHAR(50),
  status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Users & Auth
CREATE TABLE IF NOT EXISTS app_user (
  id             BIGSERIAL PRIMARY KEY,
  tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
  username       VARCHAR(100) UNIQUE NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  email          VARCHAR(200),
  status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_at      TIMESTAMP,
  last_login_at  TIMESTAMP,
  created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_profile (
  user_id      BIGINT PRIMARY KEY REFERENCES app_user(id),
  first_name   VARCHAR(100),
  last_name    VARCHAR(100),
  phone        VARCHAR(50),
  avatar_url   VARCHAR(500)
);

-- RBAC
CREATE TABLE IF NOT EXISTS role (
  id           BIGSERIAL PRIMARY KEY,
  code         VARCHAR(100) UNIQUE NOT NULL,
  name         VARCHAR(200) NOT NULL,
  description  VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS permission (
  id           BIGSERIAL PRIMARY KEY,
  code         VARCHAR(100) UNIQUE NOT NULL,
  description  VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS tenant_role_permission (
  id             BIGSERIAL PRIMARY KEY,
  tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
  role_id        BIGINT NOT NULL REFERENCES role(id),
  permission_id  BIGINT NOT NULL REFERENCES permission(id),
  UNIQUE (tenant_id, role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS user_role (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES app_user(id),
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  branch_id    BIGINT REFERENCES branch(id),
  role_id      BIGINT NOT NULL REFERENCES role(id),
  UNIQUE (user_id, tenant_id, branch_id, role_id)
);

-- Common Masters
CREATE TABLE IF NOT EXISTS category_details (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  name         VARCHAR(100) NOT NULL,
  active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS grade_details (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  name         VARCHAR(100) NOT NULL,
  active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS uom_details (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  name         VARCHAR(50) NOT NULL,
  active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS storage_condition_details (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  value        VARCHAR(100) NOT NULL,
  active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- Supplier
CREATE TABLE IF NOT EXISTS supplier (
  id             BIGSERIAL PRIMARY KEY,
  tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
  name           VARCHAR(200) NOT NULL,
  code           VARCHAR(100),
  supplier_type  VARCHAR(50),
  email          VARCHAR(200),
  phone          VARCHAR(50),
  address        TEXT,
  status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_by     BIGINT REFERENCES app_user(id),
  created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS supplier_document (
  id             BIGSERIAL PRIMARY KEY,
  supplier_id    BIGINT NOT NULL REFERENCES supplier(id),
  doc_type       VARCHAR(50),
  file_id        BIGINT,
  version        VARCHAR(50),
  expiry_date    DATE,
  uploaded_by    BIGINT REFERENCES app_user(id),
  uploaded_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS supplier_rating (
  id             BIGSERIAL PRIMARY KEY,
  supplier_id    BIGINT NOT NULL REFERENCES supplier(id),
  rating         INT CHECK (rating BETWEEN 1 AND 5),
  remarks        TEXT,
  rated_by       BIGINT REFERENCES app_user(id),
  rated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Chemical Module
CREATE TABLE IF NOT EXISTS chemical_master (
  id                  BIGSERIAL PRIMARY KEY,
  tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
  name                VARCHAR(200) NOT NULL,
  cas_no              VARCHAR(100),
  default_category_id BIGINT REFERENCES category_details(id),
  default_grade_id    BIGINT REFERENCES grade_details(id),
  hazard_class        VARCHAR(100),
  ghs_pictograms      VARCHAR(200),
  nfpa_rating         VARCHAR(50),
  sds_file_id         BIGINT,
  active              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS chemical_registration (
  id                    BIGSERIAL PRIMARY KEY,
  tenant_id             BIGINT NOT NULL REFERENCES tenant(id),
  branch_id             BIGINT NOT NULL REFERENCES branch(id),
  chemical_id           BIGINT NOT NULL REFERENCES chemical_master(id),
  category_id           BIGINT REFERENCES category_details(id),
  grade_id              BIGINT REFERENCES grade_details(id),
  cas_cat_no            VARCHAR(100),
  manufacturer_id       BIGINT REFERENCES supplier(id),
  supplier_id           BIGINT REFERENCES supplier(id),
  lot_no                VARCHAR(100),
  delivery_receipt_no   VARCHAR(100),
  no_of_containers      INT NOT NULL,
  uom_id                BIGINT NOT NULL REFERENCES uom_details(id),
  quantity_received     DECIMAL(18,4) NOT NULL,
  storage_condition_id  BIGINT REFERENCES storage_condition_details(id),
  mfg_date              DATE,
  expiry_date           DATE,
  received_date         DATE,
  received_by           BIGINT REFERENCES app_user(id),
  reg_no                VARCHAR(50) UNIQUE NOT NULL,
  status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS chemical_stock (
  id                  BIGSERIAL PRIMARY KEY,
  tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
  branch_id           BIGINT NOT NULL REFERENCES branch(id),
  registration_id     BIGINT NOT NULL REFERENCES chemical_registration(id),
  containers_in_stock INT NOT NULL,
  quantity_in_stock   DECIMAL(18,4) NOT NULL,
  status              VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  last_updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chemical_issuance (
  id                  BIGSERIAL PRIMARY KEY,
  tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
  branch_id           BIGINT NOT NULL REFERENCES branch(id),
  registration_id     BIGINT NOT NULL REFERENCES chemical_registration(id),
  containers_issued   INT NOT NULL,
  issued_quantity     DECIMAL(18,4) NOT NULL,
  uom_id              BIGINT NOT NULL REFERENCES uom_details(id),
  issued_to           BIGINT REFERENCES app_user(id),
  issued_by           BIGINT REFERENCES app_user(id),
  issued_date         TIMESTAMP NOT NULL DEFAULT NOW(),
  purpose             VARCHAR(200),
  remarks             TEXT
);

CREATE TABLE IF NOT EXISTS chemical_destruction (
  id                    BIGSERIAL PRIMARY KEY,
  tenant_id             BIGINT NOT NULL REFERENCES tenant(id),
  branch_id             BIGINT NOT NULL REFERENCES branch(id),
  registration_id       BIGINT NOT NULL REFERENCES chemical_registration(id),
  containers_destroyed  INT NOT NULL,
  quantity_destroyed    DECIMAL(18,4) NOT NULL,
  uom_id                BIGINT NOT NULL REFERENCES uom_details(id),
  destroyed_by          BIGINT REFERENCES app_user(id),
  destruction_date      TIMESTAMP NOT NULL DEFAULT NOW(),
  method                VARCHAR(200),
  witnessed_by          BIGINT REFERENCES app_user(id),
  remarks               TEXT
);

-- Instrument Module
CREATE TABLE IF NOT EXISTS instrument_category (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  name         VARCHAR(100) NOT NULL,
  active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS instrument_master (
  id              BIGSERIAL PRIMARY KEY,
  tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
  branch_id       BIGINT NOT NULL REFERENCES branch(id),
  category_id     BIGINT REFERENCES instrument_category(id),
  instrument_code VARCHAR(100) UNIQUE NOT NULL,
  name            VARCHAR(200) NOT NULL,
  serial_no       VARCHAR(200),
  model           VARCHAR(200),
  make            VARCHAR(200),
  installed_at    VARCHAR(200),
  installed_on    DATE,
  department      VARCHAR(200),
  supplier_id     BIGINT REFERENCES supplier(id),
  warranty_expiry DATE,
  status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  remarks         TEXT
);

CREATE TABLE IF NOT EXISTS instrument_calibration_schedule (
  id                  BIGSERIAL PRIMARY KEY,
  instrument_id       BIGINT NOT NULL REFERENCES instrument_master(id),
  frequency_months    INT NOT NULL,
  tolerance_days      INT NOT NULL DEFAULT 7,
  next_due_date       DATE NOT NULL,
  last_calibrated_on  DATE,
  status              VARCHAR(20) NOT NULL DEFAULT 'UPCOMING'
);

CREATE TABLE IF NOT EXISTS instrument_calibration (
  id                    BIGSERIAL PRIMARY KEY,
  instrument_id         BIGINT NOT NULL REFERENCES instrument_master(id),
  tenant_id             BIGINT NOT NULL REFERENCES tenant(id),
  branch_id             BIGINT NOT NULL REFERENCES branch(id),
  scheduled             BOOLEAN NOT NULL DEFAULT TRUE,
  calibration_due_date  DATE,
  calibrated_on         DATE,
  analyst_id            BIGINT REFERENCES app_user(id),
  reviewer_id           BIGINT REFERENCES app_user(id),
  manager_id            BIGINT REFERENCES app_user(id),
  status                VARCHAR(30) NOT NULL DEFAULT 'DUE_FOR_ANALYSIS',
  remarks               TEXT,
  created_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS instrument_test_template (
  id                      BIGSERIAL PRIMARY KEY,
  tenant_id               BIGINT NOT NULL REFERENCES tenant(id),
  instrument_category_id  BIGINT REFERENCES instrument_category(id),
  test_name               VARCHAR(200) NOT NULL,
  spec_min                DECIMAL(18,4),
  spec_max                DECIMAL(18,4),
  uom                     VARCHAR(50),
  active                  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS instrument_calibration_result (
  id              BIGSERIAL PRIMARY KEY,
  calibration_id  BIGINT NOT NULL REFERENCES instrument_calibration(id),
  template_id     BIGINT NOT NULL REFERENCES instrument_test_template(id),
  observation     DECIMAL(18,4),
  acquired_time   TIMESTAMP NOT NULL DEFAULT NOW(),
  pass_fail       VARCHAR(10),
  remarks         TEXT
);

CREATE TABLE IF NOT EXISTS instrument_calibration_status_history (
  id              BIGSERIAL PRIMARY KEY,
  calibration_id  BIGINT NOT NULL REFERENCES instrument_calibration(id),
  status          VARCHAR(30) NOT NULL,
  changed_by      BIGINT REFERENCES app_user(id),
  changed_on      TIMESTAMP NOT NULL DEFAULT NOW(),
  remarks         TEXT
);

CREATE TABLE IF NOT EXISTS instrument_maintenance (
  id                BIGSERIAL PRIMARY KEY,
  instrument_id     BIGINT NOT NULL REFERENCES instrument_master(id),
  maintenance_date  DATE NOT NULL,
  type              VARCHAR(100),
  vendor_id         BIGINT REFERENCES supplier(id),
  remarks           TEXT,
  status            VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE IF NOT EXISTS instrument_downtime (
  id              BIGSERIAL PRIMARY KEY,
  instrument_id   BIGINT NOT NULL REFERENCES instrument_master(id),
  start_time      TIMESTAMP NOT NULL,
  end_time        TIMESTAMP,
  reason          TEXT,
  remarks         TEXT
);

-- OMS
CREATE TABLE IF NOT EXISTS purchase_order (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  branch_id    BIGINT NOT NULL REFERENCES branch(id),
  po_no        VARCHAR(100) NOT NULL,
  supplier_id  BIGINT NOT NULL REFERENCES supplier(id),
  status       VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  created_by   BIGINT REFERENCES app_user(id),
  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
  approved_by  BIGINT REFERENCES app_user(id),
  approved_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase_order_line (
  id            BIGSERIAL PRIMARY KEY,
  po_id         BIGINT NOT NULL REFERENCES purchase_order(id),
  item_type     VARCHAR(20) NOT NULL,
  item_id       BIGINT NOT NULL,
  quantity      DECIMAL(18,4) NOT NULL,
  uom_id        BIGINT REFERENCES uom_details(id),
  expected_date DATE
);

CREATE TABLE IF NOT EXISTS goods_receipt (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  branch_id    BIGINT NOT NULL REFERENCES branch(id),
  po_id        BIGINT NOT NULL REFERENCES purchase_order(id),
  grn_no       VARCHAR(100) NOT NULL,
  received_at  TIMESTAMP NOT NULL DEFAULT NOW(),
  received_by  BIGINT REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS goods_receipt_line (
  id                  BIGSERIAL PRIMARY KEY,
  grn_id              BIGINT NOT NULL REFERENCES goods_receipt(id),
  po_line_id          BIGINT NOT NULL REFERENCES purchase_order_line(id),
  received_qty        DECIMAL(18,4) NOT NULL,
  inventory_stock_id  BIGINT,
  remarks             TEXT
);

-- QA/QC
CREATE TABLE IF NOT EXISTS deviation (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  branch_id    BIGINT NOT NULL REFERENCES branch(id),
  ref_entity   VARCHAR(50),
  ref_id       BIGINT,
  description  TEXT NOT NULL,
  severity     VARCHAR(20),
  status       VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  raised_by    BIGINT REFERENCES app_user(id),
  raised_at    TIMESTAMP NOT NULL DEFAULT NOW(),
  closed_by    BIGINT REFERENCES app_user(id),
  closed_at    TIMESTAMP,
  remarks      TEXT
);

CREATE TABLE IF NOT EXISTS oos_case (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  branch_id    BIGINT NOT NULL REFERENCES branch(id),
  sample_id    BIGINT,
  test_id      BIGINT,
  description  TEXT,
  status       VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  raised_by    BIGINT REFERENCES app_user(id),
  raised_at    TIMESTAMP NOT NULL DEFAULT NOW(),
  closed_by    BIGINT REFERENCES app_user(id),
  closed_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS capa (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  deviation_id BIGINT REFERENCES deviation(id),
  action_desc  TEXT NOT NULL,
  owner_id     BIGINT REFERENCES app_user(id),
  due_date     DATE,
  status       VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  completed_at TIMESTAMP,
  remarks      TEXT
);

-- Sample & Test
CREATE TABLE IF NOT EXISTS sample (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  branch_id    BIGINT NOT NULL REFERENCES branch(id),
  sample_no    VARCHAR(100) NOT NULL,
  sample_type  VARCHAR(100),
  product_name VARCHAR(200),
  batch_no     VARCHAR(100),
  received_at  TIMESTAMP,
  status       VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
  created_by   BIGINT REFERENCES app_user(id),
  created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS test_definition (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  code         VARCHAR(100) NOT NULL,
  name         VARCHAR(200) NOT NULL,
  method_id    BIGINT,
  matrix       VARCHAR(100),
  unit         VARCHAR(50),
  spec_min     DECIMAL(18,4),
  spec_max     DECIMAL(18,4),
  status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS sample_test (
  id           BIGSERIAL PRIMARY KEY,
  sample_id    BIGINT NOT NULL REFERENCES sample(id),
  test_def_id  BIGINT NOT NULL REFERENCES test_definition(id),
  assigned_to  BIGINT REFERENCES app_user(id),
  status       VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
  started_at   TIMESTAMP,
  completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS test_result (
  id              BIGSERIAL PRIMARY KEY,
  sample_test_id  BIGINT NOT NULL REFERENCES sample_test(id),
  parameter_name  VARCHAR(200) NOT NULL,
  result_value    VARCHAR(200),
  numeric_value   DECIMAL(18,4),
  unit            VARCHAR(50),
  status          VARCHAR(30) NOT NULL DEFAULT 'ENTERED',
  entered_by      BIGINT REFERENCES app_user(id),
  entered_at      TIMESTAMP,
  reviewed_by     BIGINT REFERENCES app_user(id),
  reviewed_at     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coa (
  id           BIGSERIAL PRIMARY KEY,
  sample_id    BIGINT NOT NULL REFERENCES sample(id),
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  branch_id    BIGINT NOT NULL REFERENCES branch(id),
  coa_no       VARCHAR(100) NOT NULL,
  status       VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  generated_at TIMESTAMP,
  approved_by  BIGINT REFERENCES app_user(id),
  approved_at  TIMESTAMP
);

-- Dashboard, AI, Notifications
CREATE TABLE IF NOT EXISTS critical_alert (
  id             BIGSERIAL PRIMARY KEY,
  tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
  branch_id      BIGINT NOT NULL REFERENCES branch(id),
  alert_type     VARCHAR(100),
  severity       VARCHAR(20),
  source_service VARCHAR(100),
  entity_type    VARCHAR(100),
  entity_id      BIGINT,
  message        TEXT,
  created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
  resolved_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_inventory_forecast (
  id               BIGSERIAL PRIMARY KEY,
  tenant_id        BIGINT NOT NULL REFERENCES tenant(id),
  branch_id        BIGINT NOT NULL REFERENCES branch(id),
  item_type        VARCHAR(20) NOT NULL,
  item_id          BIGINT NOT NULL,
  forecast_date    DATE NOT NULL,
  predicted_usage  DECIMAL(18,4),
  model_version    VARCHAR(50),
  created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notification (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  user_id      BIGINT REFERENCES app_user(id),
  type         VARCHAR(50),
  title        VARCHAR(200),
  message      TEXT,
  status       VARCHAR(20) DEFAULT 'PENDING',
  created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_log (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  user_id      BIGINT REFERENCES app_user(id),
  entity_type  VARCHAR(100) NOT NULL,
  entity_id    BIGINT NOT NULL,
  action       VARCHAR(50) NOT NULL,
  old_value    TEXT,
  new_value    TEXT,
  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
  trace_id     VARCHAR(64),
  span_id      VARCHAR(64)
);

-- Document Module
CREATE TABLE IF NOT EXISTS document_master (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
  name         VARCHAR(200) NOT NULL,
  type         VARCHAR(50) NOT NULL,
  version      INT NOT NULL DEFAULT 1,
  file_id      BIGINT,
  status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  uploaded_by  BIGINT REFERENCES app_user(id),
  uploaded_at  TIMESTAMP DEFAULT NOW(),
  UNIQUE (tenant_id, name, version)
);

CREATE TABLE IF NOT EXISTS document_history (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES document_master(id),
  version      INT NOT NULL,
  file_id      BIGINT NOT NULL,
  archived_at  TIMESTAMP DEFAULT NOW(),
  archived_by  BIGINT REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS document_parsed_json (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES document_master(id),
  version      INT NOT NULL,
  parsed_json  JSONB NOT NULL,
  created_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS worksheet_execution (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES document_master(id),
  version      INT NOT NULL,
  sample_id    BIGINT,
  executed_by  BIGINT REFERENCES app_user(id),
  executed_at  TIMESTAMP DEFAULT NOW(),
  status       VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
  filled_json  JSONB NOT NULL
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_app_user_tenant ON app_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_app_user_username ON app_user(username);
CREATE INDEX IF NOT EXISTS idx_chemical_reg_tenant_branch ON chemical_registration(tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_chemical_stock_reg ON chemical_stock(registration_id);
CREATE INDEX IF NOT EXISTS idx_instrument_tenant_branch ON instrument_master(tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_calibration_instrument ON instrument_calibration(instrument_id);
CREATE INDEX IF NOT EXISTS idx_sample_tenant_branch ON sample(tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant ON audit_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON audit_log(entity_type, entity_id);
