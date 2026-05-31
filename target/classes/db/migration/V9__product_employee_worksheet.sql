/*
  # V9 - Product Registration, Employee Management, and Worksheet Management

  All tables are fully tenant_id + branch_id scoped.
  All queries on these tables MUST include both tenant_id and branch_id predicates.

  ## New Tables

  1. Product Module
     - `product_master`         Core product identity, regulatory, manufacturing details
     - `product_composition`    Bill of Materials (ingredients per product)
     - `product_specification`  Test methods, release criteria, stability info
     - `product_attachments`    File references (COA template, MSDS, SDS, label)
     - `product_workflow`       Lifecycle audit: DRAFT → UNDER_REVIEW → APPROVED / REJECTED
     - `product_audit`          Field-level change tracking

  2. Employee Module
     - `employee_master`        Employee profiles linked to app_user login accounts
     - `employee_hierarchy`     Multi-level reporting relationships
     - `employee_audit`         Field-level change tracking for employee records

  3. Worksheet Module
     - `worksheet_master`       Product+batch worksheets with full lifecycle
     - `worksheet_execution_data` Individual field values recorded during QC execution
     - `worksheet_review_history` Immutable status-change audit trail

  ## Security
  - RLS enabled on every table
  - All policies check tenant membership via app_user lookup
*/

-- ============================================================
-- PRODUCT MODULE
-- ============================================================

CREATE TABLE IF NOT EXISTS product_master (
                                              product_id            BIGSERIAL PRIMARY KEY,
                                              tenant_id             BIGINT NOT NULL REFERENCES tenant(id),
    branch_id             BIGINT NOT NULL REFERENCES branch(id),

    product_code          VARCHAR(100) NOT NULL,
    product_name          VARCHAR(255) NOT NULL,
    product_type          VARCHAR(50)  NOT NULL
    CHECK (product_type IN ('Drug Product','Raw Material','Intermediate','API','Packaging Material')),
    strength              VARCHAR(100),
    dosage_form           VARCHAR(100),
    batch_size            NUMERIC(18,3),
    batch_uom             VARCHAR(50),

    hsn_code              VARCHAR(50),
    therapeutic_category  VARCHAR(100),
    regulatory_status     VARCHAR(50)  DEFAULT 'Under Review'
    CHECK (regulatory_status IN ('Approved','Under Review','Experimental')),
    shelf_life_value      INT,
    shelf_life_unit       VARCHAR(20)
    CHECK (shelf_life_unit IN ('Months','Years')),
    storage_condition     VARCHAR(100),

    manufacturer_id       BIGINT REFERENCES supplier(id),
    site_id               BIGINT,
    production_line_id    BIGINT,

    primary_packaging     VARCHAR(200),
    secondary_packaging   VARCHAR(200),
    label_template_path   TEXT,

    sampling_plan         VARCHAR(100),
    sample_quantity       NUMERIC(18,3),
    sample_uom            VARCHAR(50),
    qc_reviewer_id        BIGINT REFERENCES app_user(id),
    qc_manager_id         BIGINT REFERENCES app_user(id),

    status                VARCHAR(50)  NOT NULL DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT','UNDER_REVIEW','APPROVED','REJECTED')),
    review_comments       TEXT,
    approval_comments     TEXT,

    created_by            BIGINT REFERENCES app_user(id),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_by           BIGINT REFERENCES app_user(id),
    modified_at           TIMESTAMP,

    UNIQUE (tenant_id, product_code)
    );

CREATE INDEX IF NOT EXISTS idx_product_master_tenant_branch
    ON product_master(tenant_id, branch_id);

CREATE INDEX IF NOT EXISTS idx_product_master_status
    ON product_master(tenant_id, branch_id, status);

ALTER TABLE product_master ENABLE ROW LEVEL SECURITY;

CREATE POLICY "product_master_select"
    ON product_master FOR SELECT TO authenticated
                                     USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "product_master_insert"
    ON product_master FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "product_master_update"
    ON product_master FOR UPDATE TO authenticated
                                            USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint))
                          WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ── Product Composition (BOM) ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_composition (
                                                   composition_id      BIGSERIAL PRIMARY KEY,
                                                   product_id          BIGINT NOT NULL REFERENCES product_master(product_id),
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    branch_id           BIGINT NOT NULL REFERENCES branch(id),
    ingredient_id       BIGINT NOT NULL REFERENCES chemical_master(id),
    ingredient_quantity NUMERIC(18,3),
    ingredient_uom      VARCHAR(50),
    ingredient_grade    VARCHAR(50),
    created_by          BIGINT REFERENCES app_user(id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_product_composition_product
    ON product_composition(product_id);

CREATE INDEX IF NOT EXISTS idx_product_composition_tenant_branch
    ON product_composition(tenant_id, branch_id);

ALTER TABLE product_composition ENABLE ROW LEVEL SECURITY;

CREATE POLICY "product_composition_select"
    ON product_composition FOR SELECT TO authenticated
                                          USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "product_composition_insert"
    ON product_composition FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "product_composition_delete"
    ON product_composition FOR DELETE TO authenticated
    USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ── Product Specification ──────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_specification (
                                                     spec_id               BIGSERIAL PRIMARY KEY,
                                                     product_id            BIGINT NOT NULL REFERENCES product_master(product_id),
    tenant_id             BIGINT NOT NULL REFERENCES tenant(id),
    branch_id             BIGINT NOT NULL REFERENCES branch(id),
    spec_document_path    TEXT,
    test_methods          TEXT,  -- JSON array of test method codes
    release_criteria      TEXT,
    stability_requirements TEXT,
    created_by            BIGINT REFERENCES app_user(id),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_by           BIGINT REFERENCES app_user(id),
    modified_at           TIMESTAMP
    );

CREATE UNIQUE INDEX IF NOT EXISTS idx_product_specification_product
    ON product_specification(product_id);

CREATE INDEX IF NOT EXISTS idx_product_specification_tenant_branch
    ON product_specification(tenant_id, branch_id);

ALTER TABLE product_specification ENABLE ROW LEVEL SECURITY;

CREATE POLICY "product_specification_select"
    ON product_specification FOR SELECT TO authenticated
                                            USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "product_specification_insert"
    ON product_specification FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "product_specification_update"
    ON product_specification FOR UPDATE TO authenticated
                                                   USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint))
                                 WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ── Product Attachments ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_attachments (
                                                   attachment_id  BIGSERIAL PRIMARY KEY,
                                                   product_id     BIGINT NOT NULL REFERENCES product_master(product_id),
    tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
    branch_id      BIGINT NOT NULL REFERENCES branch(id),
    file_name      VARCHAR(255),
    file_type      VARCHAR(100),   -- COA_TEMPLATE, MSDS, SDS, LABEL, OTHER
    file_path      TEXT,
    uploaded_by    BIGINT REFERENCES app_user(id),
    uploaded_at    TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_product_attachments_product
    ON product_attachments(product_id);

CREATE INDEX IF NOT EXISTS idx_product_attachments_tenant_branch
    ON product_attachments(tenant_id, branch_id);

ALTER TABLE product_attachments ENABLE ROW LEVEL SECURITY;

CREATE POLICY "product_attachments_select"
    ON product_attachments FOR SELECT TO authenticated
                                          USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "product_attachments_insert"
    ON product_attachments FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "product_attachments_delete"
    ON product_attachments FOR DELETE TO authenticated
    USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ── Product Workflow (lifecycle audit) ────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_workflow (
                                                workflow_id  BIGSERIAL PRIMARY KEY,
                                                product_id   BIGINT NOT NULL REFERENCES product_master(product_id),
    tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
    branch_id    BIGINT NOT NULL REFERENCES branch(id),
    old_status   VARCHAR(50),
    new_status   VARCHAR(50) NOT NULL,
    action_by    BIGINT REFERENCES app_user(id),
    action_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    comments     TEXT
    );

CREATE INDEX IF NOT EXISTS idx_product_workflow_product
    ON product_workflow(product_id);

CREATE INDEX IF NOT EXISTS idx_product_workflow_tenant_branch
    ON product_workflow(tenant_id, branch_id);

ALTER TABLE product_workflow ENABLE ROW LEVEL SECURITY;

CREATE POLICY "product_workflow_select"
    ON product_workflow FOR SELECT TO authenticated
                                       USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "product_workflow_insert"
    ON product_workflow FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ── Product Audit (field-level) ────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS product_audit (
                                             audit_id    BIGSERIAL PRIMARY KEY,
                                             product_id  BIGINT NOT NULL REFERENCES product_master(product_id),
    tenant_id   BIGINT NOT NULL REFERENCES tenant(id),
    branch_id   BIGINT NOT NULL REFERENCES branch(id),
    field_name  VARCHAR(255),
    old_value   TEXT,
    new_value   TEXT,
    changed_by  BIGINT REFERENCES app_user(id),
    changed_at  TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_product_audit_product
    ON product_audit(product_id);

CREATE INDEX IF NOT EXISTS idx_product_audit_tenant_branch
    ON product_audit(tenant_id, branch_id);

ALTER TABLE product_audit ENABLE ROW LEVEL SECURITY;

CREATE POLICY "product_audit_select"
    ON product_audit FOR SELECT TO authenticated
                                    USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "product_audit_insert"
    ON product_audit FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ============================================================
-- EMPLOYEE MODULE
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_master (
                                               employee_id    BIGSERIAL PRIMARY KEY,
                                               tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
    branch_id      BIGINT NOT NULL REFERENCES branch(id),

    employee_code  VARCHAR(50) NOT NULL,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100),
    email          VARCHAR(255) NOT NULL,
    phone          VARCHAR(50),

    login_user_id  BIGINT NOT NULL REFERENCES app_user(id),
    role_id        BIGINT NOT NULL REFERENCES role(id),
    designation    VARCHAR(100),

    manager_id     BIGINT REFERENCES employee_master(employee_id),
    reviewer_id    BIGINT REFERENCES employee_master(employee_id),

    is_active      BOOLEAN NOT NULL DEFAULT TRUE,

    created_by     BIGINT REFERENCES app_user(id),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_by    BIGINT REFERENCES app_user(id),
    modified_at    TIMESTAMP,

    UNIQUE (tenant_id, employee_code),
    UNIQUE (tenant_id, email)
    );

CREATE INDEX IF NOT EXISTS idx_employee_master_tenant_branch
    ON employee_master(tenant_id, branch_id);

CREATE INDEX IF NOT EXISTS idx_employee_master_login_user
    ON employee_master(login_user_id);

CREATE INDEX IF NOT EXISTS idx_employee_master_manager
    ON employee_master(manager_id);

ALTER TABLE employee_master ENABLE ROW LEVEL SECURITY;

CREATE POLICY "employee_master_select"
    ON employee_master FOR SELECT TO authenticated
                                      USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "employee_master_insert"
    ON employee_master FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "employee_master_update"
    ON employee_master FOR UPDATE TO authenticated
                                             USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint))
                           WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ── Employee Hierarchy ─────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS employee_hierarchy (
                                                  id           BIGSERIAL PRIMARY KEY,
                                                  tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
    branch_id    BIGINT NOT NULL REFERENCES branch(id),
    employee_id  BIGINT NOT NULL REFERENCES employee_master(employee_id),
    manager_id   BIGINT NOT NULL REFERENCES employee_master(employee_id),
    level        INT NOT NULL DEFAULT 1,  -- 1 = direct manager, 2 = skip level
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE (employee_id, manager_id)
    );

CREATE INDEX IF NOT EXISTS idx_employee_hierarchy_employee
    ON employee_hierarchy(employee_id);

CREATE INDEX IF NOT EXISTS idx_employee_hierarchy_manager
    ON employee_hierarchy(manager_id);

CREATE INDEX IF NOT EXISTS idx_employee_hierarchy_tenant_branch
    ON employee_hierarchy(tenant_id, branch_id);

ALTER TABLE employee_hierarchy ENABLE ROW LEVEL SECURITY;

CREATE POLICY "employee_hierarchy_select"
    ON employee_hierarchy FOR SELECT TO authenticated
                                         USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "employee_hierarchy_insert"
    ON employee_hierarchy FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "employee_hierarchy_delete"
    ON employee_hierarchy FOR DELETE TO authenticated
    USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ── Employee Audit ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS employee_audit (
                                              audit_id     BIGSERIAL PRIMARY KEY,
                                              tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
    branch_id    BIGINT NOT NULL REFERENCES branch(id),
    employee_id  BIGINT NOT NULL REFERENCES employee_master(employee_id),
    field_name   VARCHAR(255),
    old_value    TEXT,
    new_value    TEXT,
    changed_by   BIGINT REFERENCES app_user(id),
    changed_at   TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_employee_audit_employee
    ON employee_audit(employee_id);

CREATE INDEX IF NOT EXISTS idx_employee_audit_tenant_branch
    ON employee_audit(tenant_id, branch_id);

ALTER TABLE employee_audit ENABLE ROW LEVEL SECURITY;

CREATE POLICY "employee_audit_select"
    ON employee_audit FOR SELECT TO authenticated
                                     USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "employee_audit_insert"
    ON employee_audit FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ============================================================
-- WORKSHEET MODULE
-- ============================================================

CREATE TABLE IF NOT EXISTS worksheet_master (
                                                worksheet_id  BIGSERIAL PRIMARY KEY,
                                                tenant_id     BIGINT NOT NULL REFERENCES tenant(id),
    branch_id     BIGINT NOT NULL REFERENCES branch(id),

    product_id    BIGINT REFERENCES product_master(product_id),
    batch_no      VARCHAR(100),
    template_id   BIGINT REFERENCES document_master(id),

    assigned_to   BIGINT REFERENCES app_user(id),
    assigned_by   BIGINT REFERENCES app_user(id),

    status        VARCHAR(50) NOT NULL DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT','IN_PROGRESS','COMPLETED','REVIEW_PENDING','APPROVED','REJECTED')),
    is_archived   BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at   TIMESTAMP,

    created_by    BIGINT REFERENCES app_user(id),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_by   BIGINT REFERENCES app_user(id),
    modified_at   TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_worksheet_master_tenant_branch
    ON worksheet_master(tenant_id, branch_id);

CREATE INDEX IF NOT EXISTS idx_worksheet_master_status
    ON worksheet_master(tenant_id, branch_id, status);

CREATE INDEX IF NOT EXISTS idx_worksheet_master_product
    ON worksheet_master(product_id);

CREATE INDEX IF NOT EXISTS idx_worksheet_master_assigned_to
    ON worksheet_master(assigned_to);

CREATE INDEX IF NOT EXISTS idx_worksheet_master_archived
    ON worksheet_master(tenant_id, branch_id, is_archived);

ALTER TABLE worksheet_master ENABLE ROW LEVEL SECURITY;

CREATE POLICY "worksheet_master_select"
    ON worksheet_master FOR SELECT TO authenticated
                                       USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "worksheet_master_insert"
    ON worksheet_master FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "worksheet_master_update"
    ON worksheet_master FOR UPDATE TO authenticated
                                              USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint))
                            WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ── Worksheet Execution Data ───────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS worksheet_execution_data (
                                                        id            BIGSERIAL PRIMARY KEY,
                                                        worksheet_id  BIGINT NOT NULL REFERENCES worksheet_master(worksheet_id),
    tenant_id     BIGINT NOT NULL REFERENCES tenant(id),
    branch_id     BIGINT NOT NULL REFERENCES branch(id),
    field_id      BIGINT,
    field_name    VARCHAR(255),
    value         TEXT,
    unit          VARCHAR(50),
    chemical_id   BIGINT REFERENCES chemical_master(id),
    instrument_id BIGINT REFERENCES instrument_master(id),
    comment       TEXT,
    reason        TEXT,
    created_by    BIGINT REFERENCES app_user(id),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_by   BIGINT REFERENCES app_user(id),
    modified_at   TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_worksheet_execution_data_worksheet
    ON worksheet_execution_data(worksheet_id);

CREATE INDEX IF NOT EXISTS idx_worksheet_execution_data_tenant_branch
    ON worksheet_execution_data(tenant_id, branch_id);

ALTER TABLE worksheet_execution_data ENABLE ROW LEVEL SECURITY;

CREATE POLICY "worksheet_execution_data_select"
    ON worksheet_execution_data FOR SELECT TO authenticated
                                               USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "worksheet_execution_data_insert"
    ON worksheet_execution_data FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "worksheet_execution_data_update"
    ON worksheet_execution_data FOR UPDATE TO authenticated
                                                      USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint))
                                    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ── Worksheet Review History ───────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS worksheet_review_history (
                                                        review_id     BIGSERIAL PRIMARY KEY,
                                                        worksheet_id  BIGINT NOT NULL REFERENCES worksheet_master(worksheet_id),
    tenant_id     BIGINT NOT NULL REFERENCES tenant(id),
    branch_id     BIGINT NOT NULL REFERENCES branch(id),
    old_status    VARCHAR(50),
    new_status    VARCHAR(50) NOT NULL,
    action_by     BIGINT REFERENCES app_user(id),
    action_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    comments      TEXT
    );

CREATE INDEX IF NOT EXISTS idx_worksheet_review_history_worksheet
    ON worksheet_review_history(worksheet_id);

CREATE INDEX IF NOT EXISTS idx_worksheet_review_history_tenant_branch
    ON worksheet_review_history(tenant_id, branch_id);

ALTER TABLE worksheet_review_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "worksheet_review_history_select"
    ON worksheet_review_history FOR SELECT TO authenticated
                                               USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "worksheet_review_history_insert"
    ON worksheet_review_history FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO permission (code, description) VALUES
                                               ('PRODUCT_VIEW',          'View products'),
                                               ('PRODUCT_CREATE',        'Create products'),
                                               ('PRODUCT_EDIT',          'Edit products'),
                                               ('PRODUCT_APPROVE',       'Approve/reject products'),
                                               ('PRODUCT_COMPOSITION_EDIT', 'Edit product BOM/composition'),
                                               ('PRODUCT_SPEC_EDIT',     'Edit product specification'),
                                               ('PRODUCT_ATTACH_UPLOAD', 'Upload product attachments'),
                                               ('EMPLOYEE_VIEW',         'View employees'),
                                               ('EMPLOYEE_CREATE',       'Create employees'),
                                               ('EMPLOYEE_EDIT',         'Edit employees'),
                                               ('EMPLOYEE_DEACTIVATE',   'Deactivate employees'),
                                               ('WORKSHEET_VIEW',        'View worksheets'),
                                               ('WORKSHEET_CREATE',      'Create worksheets'),
                                               ('WORKSHEET_EXECUTE',     'Execute / fill worksheets'),
                                               ('WORKSHEET_ASSIGN',      'Assign worksheets to analysts'),
                                               ('WORKSHEET_REVIEW',      'Review worksheets'),
                                               ('WORKSHEET_APPROVE',     'Approve/reject worksheets'),
                                               ('WORKSHEET_ARCHIVE',     'Archive worksheets')
    ON CONFLICT (code) DO NOTHING;

-- Grant to SUPER_ADMIN (all)
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'SUPER_ADMIN'
  AND p.code IN (
                 'PRODUCT_VIEW','PRODUCT_CREATE','PRODUCT_EDIT','PRODUCT_APPROVE',
                 'PRODUCT_COMPOSITION_EDIT','PRODUCT_SPEC_EDIT','PRODUCT_ATTACH_UPLOAD',
                 'EMPLOYEE_VIEW','EMPLOYEE_CREATE','EMPLOYEE_EDIT','EMPLOYEE_DEACTIVATE',
                 'WORKSHEET_VIEW','WORKSHEET_CREATE','WORKSHEET_EXECUTE','WORKSHEET_ASSIGN',
                 'WORKSHEET_REVIEW','WORKSHEET_APPROVE','WORKSHEET_ARCHIVE'
    )
    ON CONFLICT DO NOTHING;

-- LAB_MANAGER
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'LAB_MANAGER'
  AND p.code IN (
                 'PRODUCT_VIEW','PRODUCT_CREATE','PRODUCT_EDIT','PRODUCT_APPROVE',
                 'PRODUCT_COMPOSITION_EDIT','PRODUCT_SPEC_EDIT','PRODUCT_ATTACH_UPLOAD',
                 'EMPLOYEE_VIEW','EMPLOYEE_CREATE','EMPLOYEE_EDIT',
                 'WORKSHEET_VIEW','WORKSHEET_CREATE','WORKSHEET_ASSIGN',
                 'WORKSHEET_REVIEW','WORKSHEET_APPROVE','WORKSHEET_ARCHIVE'
    )
    ON CONFLICT DO NOTHING;

-- QA_MANAGER / QC_MANAGER
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code IN ('QA_MANAGER','QC_MANAGER')
  AND p.code IN (
                 'PRODUCT_VIEW','PRODUCT_CREATE','PRODUCT_EDIT','PRODUCT_APPROVE',
                 'PRODUCT_COMPOSITION_EDIT','PRODUCT_SPEC_EDIT','PRODUCT_ATTACH_UPLOAD',
                 'EMPLOYEE_VIEW',
                 'WORKSHEET_VIEW','WORKSHEET_CREATE','WORKSHEET_ASSIGN',
                 'WORKSHEET_REVIEW','WORKSHEET_APPROVE','WORKSHEET_ARCHIVE'
    )
    ON CONFLICT DO NOTHING;

-- ANALYST
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'ANALYST'
  AND p.code IN ('PRODUCT_VIEW','WORKSHEET_VIEW','WORKSHEET_EXECUTE')
    ON CONFLICT DO NOTHING;

-- REVIEWER
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'REVIEWER'
  AND p.code IN ('PRODUCT_VIEW','WORKSHEET_VIEW','WORKSHEET_REVIEW')
    ON CONFLICT DO NOTHING;

-- APPROVER
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'APPROVER'
  AND p.code IN (
                 'PRODUCT_VIEW','PRODUCT_APPROVE',
                 'EMPLOYEE_VIEW',
                 'WORKSHEET_VIEW','WORKSHEET_APPROVE','WORKSHEET_ARCHIVE'
    )
    ON CONFLICT DO NOTHING;

-- VIEWER
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'VIEWER'
  AND p.code IN ('PRODUCT_VIEW','EMPLOYEE_VIEW','WORKSHEET_VIEW')
    ON CONFLICT DO NOTHING;
