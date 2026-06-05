/*
  # V10 - Worksheet Document Template & Test Case Engine

  ## Overview
  Extends the existing document_version table and adds a structured
  document-template model. Each uploaded DOCX is parsed into an ordered
  linked-list of blocks (PARAGRAPH, TABLE, IMAGE, FORMULA). The document is
  segmented into test cases — each test case ends with exactly one FORMULA block.
  Analysts fill `--` placeholder slots; results are computed per test case.

  ## Modified Tables

  1. `document_version` (already created in V4)
     - Adds: parse_status, parse_error, parsed_at, mime_type columns.
     - These track the async DOCX parse pipeline state.

  ## New Tables

  2. `document_test_case`
     - One row per test case within a document version.
     - Ordered by `test_case_index`.
     - Stores the raw formula text and a parseable formula_expression
       where `--` variables are replaced by A, B, C... references.

  3. `document_template_block`
     - One row per content block within a test case.
     - Ordered by `block_index` (this is the linked-list order).
     - block_type: PARAGRAPH | TABLE | IMAGE | FORMULA
     - content_json holds the full block content with `--` preserved as placeholders.
     - storage_path used only for IMAGE blocks.

  4. `document_field_slot`
     - One row per `--` occurrence across all blocks in a test case.
     - field_index: global sequential number within the test case (1, 2, 3...).
     - field_variable: auto-assigned variable name A, B, C... used in formula_expression.
     - label: surrounding text extracted during parse as a hint to the analyst.
     - default_unit: unit hint if the document text implies one (e.g., "ml", "g").

  5. `worksheet_field_value`
     - Analyst-supplied data per field slot.
     - numeric_value: the textbox input (decimal).
     - unit: first dropdown (ml / L / g / kg / mg / µg / mEq / IU / %).
     - qualifier: second dropdown (EXACT / APPROX / TRACE / ND).
     - Unique per (worksheet_id, slot_id) — one answer per slot; supports upsert.

  6. `worksheet_test_case_result`
     - Computed result per test case after analyst fills all slots.
     - Stores the substituted formula string, the numeric result, unit, and pass/fail.
     - reviewed_by / reviewed_at: reviewer who validated this result.

  ## Also Modified
  - `worksheet_master`: adds `document_version_id` FK to point to parsed version.

  ## Security
  - RLS enabled on all new tables.
  - All policies check tenant membership via app_user lookup using auth.uid().

  ## Important Notes
  1. document_version blocks/slots are immutable after parse — never update them.
  2. worksheet_field_value UNIQUE (worksheet_id, slot_id) allows upsert by analyst.
  3. Formula evaluation (expression → numeric result) is done in the Java service layer.
  4. block_index ordering within a test case reconstructs the LinkedList in memory.
  5. All new tables reference document_version.id (the existing PK from V4).
*/

-- ============================================================
-- EXTEND document_version (already exists from V4)
-- Add parse tracking columns needed by the new pipeline.
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'document_version' AND column_name = 'parse_status'
    ) THEN
ALTER TABLE document_version
    ADD COLUMN parse_status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
        CHECK (parse_status IN ('PENDING','PROCESSING','PARSED','FAILED'));
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'document_version' AND column_name = 'parse_error'
    ) THEN
ALTER TABLE document_version ADD COLUMN parse_error TEXT;
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'document_version' AND column_name = 'parsed_at'
    ) THEN
ALTER TABLE document_version ADD COLUMN parsed_at TIMESTAMP;
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'document_version' AND column_name = 'mime_type'
    ) THEN
ALTER TABLE document_version ADD COLUMN mime_type VARCHAR(100);
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_document_version_parse_status
    ON document_version(tenant_id, parse_status);

-- ============================================================
-- DOCUMENT TEST CASE
-- Each test case is a group of blocks ending with a FORMULA block.
-- References document_version.id (V4 PK).
-- ============================================================

CREATE TABLE IF NOT EXISTS document_test_case (
                                                  test_case_id        BIGSERIAL PRIMARY KEY,
                                                  document_version_id BIGINT NOT NULL REFERENCES document_version(id),
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    branch_id           BIGINT NOT NULL REFERENCES branch(id),

    test_case_index     INT NOT NULL,         -- ordering within document (1, 2, 3...)
    test_case_name      VARCHAR(500),         -- auto-extracted or editable label
    description         TEXT,

    -- Raw formula text as it appears in the FORMULA block
    formula_text        TEXT NOT NULL,
    -- Parseable form: -- occurrences replaced with A/B/C variable references
    formula_expression  TEXT,

    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE (document_version_id, test_case_index)
    );

CREATE INDEX IF NOT EXISTS idx_document_test_case_version
    ON document_test_case(document_version_id);
CREATE INDEX IF NOT EXISTS idx_document_test_case_tenant_branch
    ON document_test_case(tenant_id, branch_id);

ALTER TABLE document_test_case ENABLE ROW LEVEL SECURITY;

CREATE POLICY "document_test_case_select"
    ON document_test_case FOR SELECT TO authenticated
                                         USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "document_test_case_insert"
    ON document_test_case FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "document_test_case_update"
    ON document_test_case FOR UPDATE TO authenticated
                                                USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint))
                              WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ============================================================
-- DOCUMENT TEMPLATE BLOCK
-- One row per block (paragraph / table / image / formula).
-- block_index within a test_case preserves the linked-list order.
-- ============================================================

CREATE TABLE IF NOT EXISTS document_template_block (
                                                       block_id            BIGSERIAL PRIMARY KEY,
                                                       test_case_id        BIGINT NOT NULL REFERENCES document_test_case(test_case_id),
    document_version_id BIGINT NOT NULL REFERENCES document_version(id),
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    branch_id           BIGINT NOT NULL REFERENCES branch(id),

    block_index         INT NOT NULL,         -- order within test case (linked-list position)
    block_type          VARCHAR(20) NOT NULL
    CHECK (block_type IN ('PARAGRAPH','TABLE','IMAGE','FORMULA')),

    -- PARAGRAPH/FORMULA: {"text": "... -- ..."}
    -- TABLE: {"rows": [["cell","--","cell"],["cell","cell","cell"]]}
    -- IMAGE: null (use storage_path)
    content_json        JSONB,

    -- Populated only for IMAGE blocks
    storage_path        TEXT,

    UNIQUE (test_case_id, block_index)
    );

CREATE INDEX IF NOT EXISTS idx_document_template_block_test_case
    ON document_template_block(test_case_id);
CREATE INDEX IF NOT EXISTS idx_document_template_block_version
    ON document_template_block(document_version_id);
CREATE INDEX IF NOT EXISTS idx_document_template_block_tenant_branch
    ON document_template_block(tenant_id, branch_id);

ALTER TABLE document_template_block ENABLE ROW LEVEL SECURITY;

CREATE POLICY "document_template_block_select"
    ON document_template_block FOR SELECT TO authenticated
                                              USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "document_template_block_insert"
    ON document_template_block FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ============================================================
-- DOCUMENT FIELD SLOT
-- One row per -- placeholder in the document.
-- Scoped to a test case; ordered by field_index within that test case.
-- ============================================================

CREATE TABLE IF NOT EXISTS document_field_slot (
                                                   slot_id             BIGSERIAL PRIMARY KEY,
                                                   test_case_id        BIGINT NOT NULL REFERENCES document_test_case(test_case_id),
    document_version_id BIGINT NOT NULL REFERENCES document_version(id),
    block_id            BIGINT NOT NULL REFERENCES document_template_block(block_id),
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    branch_id           BIGINT NOT NULL REFERENCES branch(id),

    field_index         INT NOT NULL,         -- global sequence within test case (1, 2, 3...)
    block_local_index   INT NOT NULL DEFAULT 0, -- position of this -- within its block
    field_variable      VARCHAR(10) NOT NULL, -- A, B, C... assigned at parse time
    label               TEXT,                 -- surrounding text hint for analyst UI
    default_unit        VARCHAR(50),          -- unit hint if found near -- in document

    UNIQUE (test_case_id, field_index)
    );

CREATE INDEX IF NOT EXISTS idx_document_field_slot_test_case
    ON document_field_slot(test_case_id);
CREATE INDEX IF NOT EXISTS idx_document_field_slot_block
    ON document_field_slot(block_id);
CREATE INDEX IF NOT EXISTS idx_document_field_slot_version
    ON document_field_slot(document_version_id);
CREATE INDEX IF NOT EXISTS idx_document_field_slot_tenant_branch
    ON document_field_slot(tenant_id, branch_id);

ALTER TABLE document_field_slot ENABLE ROW LEVEL SECURITY;

CREATE POLICY "document_field_slot_select"
    ON document_field_slot FOR SELECT TO authenticated
                                          USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "document_field_slot_insert"
    ON document_field_slot FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ============================================================
-- WORKSHEET FIELD VALUE
-- Analyst fills one row per -- slot. Upsertable by (worksheet_id, slot_id).
-- ============================================================

CREATE TABLE IF NOT EXISTS worksheet_field_value (
                                                     value_id        BIGSERIAL PRIMARY KEY,
                                                     worksheet_id    BIGINT NOT NULL REFERENCES worksheet_master(worksheet_id),
    slot_id         BIGINT NOT NULL REFERENCES document_field_slot(slot_id),
    test_case_id    BIGINT NOT NULL REFERENCES document_test_case(test_case_id),
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    branch_id       BIGINT NOT NULL REFERENCES branch(id),

    -- The three inputs rendered for each -- placeholder in analyst mode
    numeric_value   NUMERIC(18,6),          -- textbox: analyst measurement
    unit            VARCHAR(50),            -- dropdown 1: ml/L/g/kg/mg/µg/mEq/IU/%
    qualifier       VARCHAR(50) NOT NULL DEFAULT 'EXACT'
    CHECK (qualifier IN ('EXACT','APPROX','TRACE','ND')),

    entered_by      BIGINT REFERENCES app_user(id),
    entered_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_by     BIGINT REFERENCES app_user(id),
    modified_at     TIMESTAMP,
    comment         TEXT,

    UNIQUE (worksheet_id, slot_id)
    );

CREATE INDEX IF NOT EXISTS idx_worksheet_field_value_worksheet
    ON worksheet_field_value(worksheet_id);
CREATE INDEX IF NOT EXISTS idx_worksheet_field_value_test_case
    ON worksheet_field_value(worksheet_id, test_case_id);
CREATE INDEX IF NOT EXISTS idx_worksheet_field_value_slot
    ON worksheet_field_value(slot_id);
CREATE INDEX IF NOT EXISTS idx_worksheet_field_value_tenant_branch
    ON worksheet_field_value(tenant_id, branch_id);

ALTER TABLE worksheet_field_value ENABLE ROW LEVEL SECURITY;

CREATE POLICY "worksheet_field_value_select"
    ON worksheet_field_value FOR SELECT TO authenticated
                                            USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "worksheet_field_value_insert"
    ON worksheet_field_value FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "worksheet_field_value_update"
    ON worksheet_field_value FOR UPDATE TO authenticated
                                                   USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint))
                                 WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ============================================================
-- WORKSHEET TEST CASE RESULT
-- One row per test case per worksheet. Computed after all slots filled.
-- ============================================================

CREATE TABLE IF NOT EXISTS worksheet_test_case_result (
                                                          result_id               BIGSERIAL PRIMARY KEY,
                                                          worksheet_id            BIGINT NOT NULL REFERENCES worksheet_master(worksheet_id),
    test_case_id            BIGINT NOT NULL REFERENCES document_test_case(test_case_id),
    tenant_id               BIGINT NOT NULL REFERENCES tenant(id),
    branch_id               BIGINT NOT NULL REFERENCES branch(id),

    -- Formula with actual values substituted, e.g. "(12.5 - 0.3) / 15.0 * 100"
    formula_substituted     TEXT,
    computed_result         NUMERIC(18,6),
    result_unit             VARCHAR(50),

    pass_fail               VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    CHECK (pass_fail IN ('PASS','FAIL','PENDING')),

    computed_by             BIGINT REFERENCES app_user(id),
    computed_at             TIMESTAMP,

    reviewed_by             BIGINT REFERENCES app_user(id),
    reviewed_at             TIMESTAMP,
    review_comments         TEXT,

    UNIQUE (worksheet_id, test_case_id)
    );

CREATE INDEX IF NOT EXISTS idx_worksheet_test_case_result_worksheet
    ON worksheet_test_case_result(worksheet_id);
CREATE INDEX IF NOT EXISTS idx_worksheet_test_case_result_test_case
    ON worksheet_test_case_result(test_case_id);
CREATE INDEX IF NOT EXISTS idx_worksheet_test_case_result_tenant_branch
    ON worksheet_test_case_result(tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_worksheet_test_case_result_pass_fail
    ON worksheet_test_case_result(worksheet_id, pass_fail);

ALTER TABLE worksheet_test_case_result ENABLE ROW LEVEL SECURITY;

CREATE POLICY "worksheet_test_case_result_select"
    ON worksheet_test_case_result FOR SELECT TO authenticated
                                                 USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "worksheet_test_case_result_insert"
    ON worksheet_test_case_result FOR INSERT TO authenticated
    WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

CREATE POLICY "worksheet_test_case_result_update"
    ON worksheet_test_case_result FOR UPDATE TO authenticated
                                                        USING (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint))
                                      WITH CHECK (tenant_id IN (SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint));

-- ============================================================
-- ALTER worksheet_master: link to parsed document version
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'worksheet_master' AND column_name = 'document_version_id'
    ) THEN
ALTER TABLE worksheet_master
    ADD COLUMN document_version_id BIGINT REFERENCES document_version(id);
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_worksheet_master_document_version
    ON worksheet_master(document_version_id);

-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT INTO permission (code, description) VALUES
                                               ('DOCUMENT_VERSION_UPLOAD',  'Upload a new document version for parsing'),
                                               ('DOCUMENT_TEMPLATE_VIEW',   'View parsed document template blocks and field slots'),
                                               ('WORKSHEET_FIELD_FILL',     'Fill worksheet field slot values as analyst'),
                                               ('WORKSHEET_RESULT_COMPUTE', 'Compute and store test case formula results'),
                                               ('WORKSHEET_RESULT_REVIEW',  'Review and approve test case results')
    ON CONFLICT (code) DO NOTHING;

-- SUPER_ADMIN
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'SUPER_ADMIN'
  AND p.code IN (
                 'DOCUMENT_VERSION_UPLOAD','DOCUMENT_TEMPLATE_VIEW',
                 'WORKSHEET_FIELD_FILL','WORKSHEET_RESULT_COMPUTE','WORKSHEET_RESULT_REVIEW'
    )
    ON CONFLICT DO NOTHING;

-- LAB_MANAGER
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'LAB_MANAGER'
  AND p.code IN (
                 'DOCUMENT_VERSION_UPLOAD','DOCUMENT_TEMPLATE_VIEW',
                 'WORKSHEET_RESULT_COMPUTE','WORKSHEET_RESULT_REVIEW'
    )
    ON CONFLICT DO NOTHING;

-- QA_MANAGER / QC_MANAGER
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code IN ('QA_MANAGER','QC_MANAGER')
  AND p.code IN (
                 'DOCUMENT_VERSION_UPLOAD','DOCUMENT_TEMPLATE_VIEW',
                 'WORKSHEET_RESULT_REVIEW'
    )
    ON CONFLICT DO NOTHING;

-- ANALYST
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'ANALYST'
  AND p.code IN ('DOCUMENT_TEMPLATE_VIEW','WORKSHEET_FIELD_FILL','WORKSHEET_RESULT_COMPUTE')
    ON CONFLICT DO NOTHING;

-- REVIEWER
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA' AND r.code = 'REVIEWER'
  AND p.code IN ('DOCUMENT_TEMPLATE_VIEW','WORKSHEET_RESULT_REVIEW')
    ON CONFLICT DO NOTHING;
