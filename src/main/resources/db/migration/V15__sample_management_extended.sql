/*
  # Sample Management Module - Extended Schema (V15)

  ## Summary
  Extends the existing sample management tables with new columns needed for full sample
  lifecycle tracking and adds new supporting tables for sample types, batches,
  test methods, specifications, test assignments, executions, release decisions,
  attachments, and audit trails.

  ## Changes to Existing Tables

  ### sample
  - Added: sample_code, sample_type_id (FK), product_id, sample_batch_id (FK),
    quantity, unit, due_date, priority, storage_location_id

  ### sample_test
  - Changed: test_def_id from NOT NULL to nullable
  - Added: test_method_id (FK), assigned_at, due_date

  ### test_result
  - Added: qualifier, oos_flag, oot_flag, remarks

  ### coa
  - Added: product_id, test_results_json, generated_by (FK), pdf_path

  ## New Tables
  1. sample_type - Catalogue of sample types with default test methods
  2. sample_type_default_test - Junction table for default test method IDs per type
  3. sample_batch - Product batches for sample grouping
  4. test_method - Lab test methods linked to SOP documents
  5. specification - Product/test method acceptance criteria (min/max/OOS/OOT)
  6. test_assignment - Assignment record per analyst per test
  7. test_execution - Instrument execution records
  8. release_decision - Final release/reject/hold decisions per sample
  9. sample_attachment - File attachments for samples
  10. sample_audit_trail - Immutable event log for each sample

  ## Security
  - RLS enabled on all new tables
  - Policies: tenanted access by tenant_id using JWT claim
*/

-- =============================================
-- ALTER existing tables
-- =============================================

-- sample: add new columns
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample' AND column_name='sample_code') THEN
        ALTER TABLE sample ADD COLUMN sample_code VARCHAR(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample' AND column_name='sample_type_id') THEN
        ALTER TABLE sample ADD COLUMN sample_type_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample' AND column_name='product_id') THEN
        ALTER TABLE sample ADD COLUMN product_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample' AND column_name='sample_batch_id') THEN
        ALTER TABLE sample ADD COLUMN sample_batch_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample' AND column_name='quantity') THEN
        ALTER TABLE sample ADD COLUMN quantity NUMERIC(18,4);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample' AND column_name='unit') THEN
        ALTER TABLE sample ADD COLUMN unit VARCHAR(50);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample' AND column_name='due_date') THEN
        ALTER TABLE sample ADD COLUMN due_date TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample' AND column_name='priority') THEN
        ALTER TABLE sample ADD COLUMN priority INTEGER NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample' AND column_name='storage_location_id') THEN
        ALTER TABLE sample ADD COLUMN storage_location_id BIGINT;
    END IF;
END $$;

-- sample_test: make test_def_id nullable, add new columns
DO $$
BEGIN
    ALTER TABLE sample_test ALTER COLUMN test_def_id DROP NOT NULL;
EXCEPTION WHEN others THEN NULL;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample_test' AND column_name='test_method_id') THEN
        ALTER TABLE sample_test ADD COLUMN test_method_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample_test' AND column_name='assigned_at') THEN
        ALTER TABLE sample_test ADD COLUMN assigned_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sample_test' AND column_name='due_date') THEN
        ALTER TABLE sample_test ADD COLUMN due_date TIMESTAMP;
    END IF;
END $$;

-- test_result: add qualifier, flags, remarks
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='test_result' AND column_name='qualifier') THEN
        ALTER TABLE test_result ADD COLUMN qualifier VARCHAR(20);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='test_result' AND column_name='oos_flag') THEN
        ALTER TABLE test_result ADD COLUMN oos_flag BOOLEAN NOT NULL DEFAULT false;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='test_result' AND column_name='oot_flag') THEN
        ALTER TABLE test_result ADD COLUMN oot_flag BOOLEAN NOT NULL DEFAULT false;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='test_result' AND column_name='remarks') THEN
        ALTER TABLE test_result ADD COLUMN remarks TEXT;
    END IF;
END $$;

-- coa: add product_id, test_results_json, generated_by, pdf_path
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='coa' AND column_name='product_id') THEN
        ALTER TABLE coa ADD COLUMN product_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='coa' AND column_name='test_results_json') THEN
        ALTER TABLE coa ADD COLUMN test_results_json TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='coa' AND column_name='generated_by') THEN
        ALTER TABLE coa ADD COLUMN generated_by BIGINT REFERENCES app_user(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='coa' AND column_name='pdf_path') THEN
        ALTER TABLE coa ADD COLUMN pdf_path VARCHAR(500);
    END IF;
END $$;

-- =============================================
-- NEW TABLES
-- =============================================

CREATE TABLE IF NOT EXISTS sample_type (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL REFERENCES tenant(id),
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    active        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sample_type_tenant_name UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS sample_type_default_test (
    sample_type_id BIGINT NOT NULL REFERENCES sample_type(id) ON DELETE CASCADE,
    test_method_id BIGINT NOT NULL,
    PRIMARY KEY (sample_type_id, test_method_id)
);

CREATE TABLE IF NOT EXISTS sample_batch (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL REFERENCES tenant(id),
    branch_id        BIGINT NOT NULL REFERENCES branch(id),
    product_id       BIGINT,
    batch_no         VARCHAR(100) NOT NULL,
    manufacture_date DATE,
    expiry_date      DATE,
    status           VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sample_batch_tenant_batch_no UNIQUE (tenant_id, batch_no)
);

CREATE TABLE IF NOT EXISTS test_method (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    sop_document_id BIGINT,
    version         VARCHAR(50),
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS specification (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
    product_id     BIGINT NOT NULL,
    test_method_id BIGINT NOT NULL,
    min_value      NUMERIC(18,4),
    max_value      NUMERIC(18,4),
    target_value   NUMERIC(18,4),
    unit           VARCHAR(50),
    oot_lower      NUMERIC(18,4),
    oot_upper      NUMERIC(18,4),
    oos_lower      NUMERIC(18,4),
    oos_upper      NUMERIC(18,4),
    active         BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_specification_tenant_product_method UNIQUE (tenant_id, product_id, test_method_id)
);

CREATE TABLE IF NOT EXISTS test_assignment (
    id             BIGSERIAL PRIMARY KEY,
    sample_test_id BIGINT NOT NULL REFERENCES sample_test(id),
    analyst_id     BIGINT NOT NULL REFERENCES app_user(id),
    assigned_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    assigned_by    BIGINT REFERENCES app_user(id),
    due_date       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS test_execution (
    id                  BIGSERIAL PRIMARY KEY,
    sample_test_id      BIGINT NOT NULL REFERENCES sample_test(id),
    instrument_id       BIGINT,
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    execution_data_json TEXT,
    comments            TEXT,
    executed_by         BIGINT REFERENCES app_user(id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS release_decision (
    id           BIGSERIAL PRIMARY KEY,
    sample_id    BIGINT NOT NULL REFERENCES sample(id),
    decision     VARCHAR(30) NOT NULL,
    decided_by   BIGINT NOT NULL REFERENCES app_user(id),
    decided_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    reason       TEXT,
    CONSTRAINT uq_release_decision_sample UNIQUE (sample_id)
);

CREATE TABLE IF NOT EXISTS sample_attachment (
    id                BIGSERIAL PRIMARY KEY,
    sample_id         BIGINT NOT NULL REFERENCES sample(id),
    file_path         VARCHAR(500) NOT NULL,
    original_filename VARCHAR(500),
    file_type         VARCHAR(100),
    file_size         BIGINT,
    uploaded_by       BIGINT REFERENCES app_user(id),
    uploaded_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sample_audit_trail (
    id             BIGSERIAL PRIMARY KEY,
    sample_id      BIGINT NOT NULL REFERENCES sample(id),
    action         VARCHAR(100) NOT NULL,
    old_value_json TEXT,
    new_value_json TEXT,
    performed_by   BIGINT REFERENCES app_user(id),
    performed_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =============================================
-- INDEXES
-- =============================================

CREATE INDEX IF NOT EXISTS idx_sample_type_tenant ON sample_type(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sample_batch_tenant_branch ON sample_batch(tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_test_method_tenant ON test_method(tenant_id);
CREATE INDEX IF NOT EXISTS idx_specification_tenant_product ON specification(tenant_id, product_id);
CREATE INDEX IF NOT EXISTS idx_test_assignment_sample_test ON test_assignment(sample_test_id);
CREATE INDEX IF NOT EXISTS idx_test_execution_sample_test ON test_execution(sample_test_id);
CREATE INDEX IF NOT EXISTS idx_sample_audit_trail_sample ON sample_audit_trail(sample_id);
CREATE INDEX IF NOT EXISTS idx_sample_attachment_sample ON sample_attachment(sample_id);

-- =============================================
-- ROW LEVEL SECURITY
-- =============================================

ALTER TABLE sample_type ENABLE ROW LEVEL SECURITY;
ALTER TABLE sample_batch ENABLE ROW LEVEL SECURITY;
ALTER TABLE test_method ENABLE ROW LEVEL SECURITY;
ALTER TABLE specification ENABLE ROW LEVEL SECURITY;
ALTER TABLE test_assignment ENABLE ROW LEVEL SECURITY;
ALTER TABLE test_execution ENABLE ROW LEVEL SECURITY;
ALTER TABLE release_decision ENABLE ROW LEVEL SECURITY;
ALTER TABLE sample_attachment ENABLE ROW LEVEL SECURITY;
ALTER TABLE sample_audit_trail ENABLE ROW LEVEL SECURITY;

-- sample_type policies
CREATE POLICY "Tenant users can view sample types"
    ON sample_type FOR SELECT TO authenticated
    USING (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

CREATE POLICY "Tenant users can insert sample types"
    ON sample_type FOR INSERT TO authenticated
    WITH CHECK (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

CREATE POLICY "Tenant users can update sample types"
    ON sample_type FOR UPDATE TO authenticated
    USING (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint))
    WITH CHECK (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

-- sample_batch policies
CREATE POLICY "Tenant users can view sample batches"
    ON sample_batch FOR SELECT TO authenticated
    USING (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

CREATE POLICY "Tenant users can insert sample batches"
    ON sample_batch FOR INSERT TO authenticated
    WITH CHECK (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

CREATE POLICY "Tenant users can update sample batches"
    ON sample_batch FOR UPDATE TO authenticated
    USING (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint))
    WITH CHECK (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

-- test_method policies
CREATE POLICY "Tenant users can view test methods"
    ON test_method FOR SELECT TO authenticated
    USING (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

CREATE POLICY "Tenant users can insert test methods"
    ON test_method FOR INSERT TO authenticated
    WITH CHECK (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

CREATE POLICY "Tenant users can update test methods"
    ON test_method FOR UPDATE TO authenticated
    USING (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint))
    WITH CHECK (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

-- specification policies
CREATE POLICY "Tenant users can view specifications"
    ON specification FOR SELECT TO authenticated
    USING (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

CREATE POLICY "Tenant users can insert specifications"
    ON specification FOR INSERT TO authenticated
    WITH CHECK (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

CREATE POLICY "Tenant users can update specifications"
    ON specification FOR UPDATE TO authenticated
    USING (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint))
    WITH CHECK (tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint));

-- test_assignment policies (join through sample_test -> sample -> tenant_id)
CREATE POLICY "Tenant users can view test assignments"
    ON test_assignment FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM sample_test st
            JOIN sample s ON s.id = st.sample_id
            WHERE st.id = test_assignment.sample_test_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );

CREATE POLICY "Tenant users can insert test assignments"
    ON test_assignment FOR INSERT TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM sample_test st
            JOIN sample s ON s.id = st.sample_id
            WHERE st.id = sample_test_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );

-- test_execution policies
CREATE POLICY "Tenant users can view test executions"
    ON test_execution FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM sample_test st
            JOIN sample s ON s.id = st.sample_id
            WHERE st.id = test_execution.sample_test_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );

CREATE POLICY "Tenant users can insert test executions"
    ON test_execution FOR INSERT TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM sample_test st
            JOIN sample s ON s.id = st.sample_id
            WHERE st.id = sample_test_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );

CREATE POLICY "Tenant users can update test executions"
    ON test_execution FOR UPDATE TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM sample_test st
            JOIN sample s ON s.id = st.sample_id
            WHERE st.id = test_execution.sample_test_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM sample_test st
            JOIN sample s ON s.id = st.sample_id
            WHERE st.id = sample_test_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );

-- release_decision policies
CREATE POLICY "Tenant users can view release decisions"
    ON release_decision FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM sample s
            WHERE s.id = release_decision.sample_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );

CREATE POLICY "Tenant users can insert release decisions"
    ON release_decision FOR INSERT TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM sample s
            WHERE s.id = sample_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );

-- sample_attachment policies
CREATE POLICY "Tenant users can view sample attachments"
    ON sample_attachment FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM sample s
            WHERE s.id = sample_attachment.sample_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );

CREATE POLICY "Tenant users can insert sample attachments"
    ON sample_attachment FOR INSERT TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM sample s
            WHERE s.id = sample_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );

-- sample_audit_trail policies
CREATE POLICY "Tenant users can view sample audit trail"
    ON sample_audit_trail FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM sample s
            WHERE s.id = sample_audit_trail.sample_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );

CREATE POLICY "Tenant users can insert sample audit trail"
    ON sample_audit_trail FOR INSERT TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM sample s
            WHERE s.id = sample_id
              AND s.tenant_id = (SELECT (auth.jwt() -> 'app_metadata' ->> 'tenant_id')::bigint)
        )
    );
