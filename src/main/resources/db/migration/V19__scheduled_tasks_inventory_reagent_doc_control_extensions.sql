/*
  # V19 - Scheduled Tasks Engine, Reagent Inventory, Document Control Extensions

  ## Summary
  This migration adds three new feature areas:
  1. A generic scheduled task engine with enum-driven type and status
  2. A reagent inventory module with FEFO lot management and movement tracking
  3. Extensions to the document_control and document_control_version tables for
     rejection workflow, renewal cycles, and effective/review dates

  ## Changes

  ### 1. New Table: scheduled_task
  - Generic task engine supporting any module: calibration, document review, training, etc.
  - task_type: CALIBRATION / SAMPLE_REVIEW / DOCUMENT_REVIEW / DOCUMENT_APPROVAL /
    STABILITY_TIMEPOINT / REAGENT_EXPIRY_CHECK / INSTRUMENT_MAINTENANCE / TRAINING /
    CAPA_ACTION / DEVIATION_REVIEW / OOS_INVESTIGATION / GENERAL
  - status: PENDING / CREATED / ACCEPTED / IN_PROGRESS / COMPLETED / APPROVED / REJECTED /
    CANCELLED / OVERDUE
  - assigned_to and executed_by FK app_user

  ### 2. New Table: inventory_reagent
  - Master reagent catalog per tenant/branch
  - reagent_code is globally unique (auto-generated prefix RGT-YYYY-XXXXX)

  ### 3. New Table: inventory_reagent_lot
  - Received lots per reagent; supports FEFO ordering by expiry_date
  - lot_number unique; status: AVAILABLE / EXHAUSTED / EXPIRED / QUARANTINE

  ### 4. New Table: inventory_movement
  - Immutable ledger of all quantity changes
  - movementType: RECEIPT / CONSUME / ADJUSTMENT_ADD / ADJUSTMENT_SUBTRACT / ADJUSTMENT_SET
  - Captures qty_before, qty_after, ref_entity, ref_id

  ### 5. Alter Table: document_control
  - Adds: category, review_due_date, review_period_months

  ### 6. Alter Table: document_control_version
  - Adds: change_summary, review_comment, approval_comment,
    rejected_by, rejected_at, rejection_reason, effective_date, review_due_date

  ### 7. New Permissions
  - TASK_CREATE, TASK_VIEW, TASK_EDIT
  - INVENTORY_VIEW, INVENTORY_MANAGE, INVENTORY_CONSUME

  ### Security
  - All new tables use standard tenant-scoped access enforced at the application layer
*/

-- ============================================================
-- 1. scheduled_task
-- ============================================================
CREATE TABLE IF NOT EXISTS scheduled_task (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    branch_id         BIGINT NOT NULL,
    task_type         VARCHAR(50) NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    ref_entity        VARCHAR(100),
    ref_id            BIGINT,
    due_date          DATE NOT NULL,
    recurrence_rule   VARCHAR(100),
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assigned_to       BIGINT REFERENCES app_user(id),
    executed_at       TIMESTAMP,
    executed_by       BIGINT REFERENCES app_user(id),
    result_notes      TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scheduled_task_tenant_status ON scheduled_task(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_scheduled_task_due_date ON scheduled_task(due_date);
CREATE INDEX IF NOT EXISTS idx_scheduled_task_assigned_to ON scheduled_task(assigned_to);
CREATE INDEX IF NOT EXISTS idx_scheduled_task_ref ON scheduled_task(ref_entity, ref_id);

-- ============================================================
-- 2. inventory_reagent
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_reagent (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL REFERENCES tenant(id),
    branch_id        BIGINT NOT NULL,
    reagent_code     VARCHAR(50) NOT NULL UNIQUE,
    name             VARCHAR(200) NOT NULL,
    category         VARCHAR(200),
    formula          VARCHAR(200),
    default_uom      VARCHAR(50),
    min_stock_level  NUMERIC(18,4),
    reorder_level    NUMERIC(18,4),
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by       BIGINT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inventory_reagent_tenant ON inventory_reagent(tenant_id, branch_id);

-- ============================================================
-- 3. inventory_reagent_lot
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_reagent_lot (
    id                BIGSERIAL PRIMARY KEY,
    reagent_id        BIGINT NOT NULL REFERENCES inventory_reagent(id),
    lot_number        VARCHAR(100) NOT NULL UNIQUE,
    supplier_lot      VARCHAR(100),
    received_qty      NUMERIC(18,4) NOT NULL,
    current_qty       NUMERIC(18,4) NOT NULL,
    uom               VARCHAR(50),
    received_date     DATE NOT NULL,
    expiry_date       DATE,
    manufacture_date  DATE,
    supplier_id       BIGINT,
    storage_location  VARCHAR(200),
    certificate_no    VARCHAR(100),
    status            VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    received_by       BIGINT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inventory_lot_reagent ON inventory_reagent_lot(reagent_id);
CREATE INDEX IF NOT EXISTS idx_inventory_lot_status ON inventory_reagent_lot(reagent_id, status);
CREATE INDEX IF NOT EXISTS idx_inventory_lot_expiry ON inventory_reagent_lot(expiry_date);

-- ============================================================
-- 4. inventory_movement
-- ============================================================
CREATE TABLE IF NOT EXISTS inventory_movement (
    id             BIGSERIAL PRIMARY KEY,
    lot_id         BIGINT NOT NULL REFERENCES inventory_reagent_lot(id),
    movement_type  VARCHAR(30) NOT NULL,
    quantity       NUMERIC(18,4) NOT NULL,
    qty_before     NUMERIC(18,4),
    qty_after      NUMERIC(18,4),
    ref_entity     VARCHAR(100),
    ref_id         BIGINT,
    reason         TEXT,
    performed_by   BIGINT NOT NULL REFERENCES app_user(id),
    performed_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inventory_movement_lot ON inventory_movement(lot_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movement_ref ON inventory_movement(ref_entity, ref_id);

-- ============================================================
-- 5. document_control column extensions
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control' AND column_name = 'category') THEN
        ALTER TABLE document_control ADD COLUMN category VARCHAR(100);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control' AND column_name = 'review_due_date') THEN
        ALTER TABLE document_control ADD COLUMN review_due_date DATE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control' AND column_name = 'review_period_months') THEN
        ALTER TABLE document_control ADD COLUMN review_period_months INT;
    END IF;
END $$;

-- ============================================================
-- 6. document_control_version column extensions
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control_version' AND column_name = 'change_summary') THEN
        ALTER TABLE document_control_version ADD COLUMN change_summary TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control_version' AND column_name = 'review_comment') THEN
        ALTER TABLE document_control_version ADD COLUMN review_comment TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control_version' AND column_name = 'approval_comment') THEN
        ALTER TABLE document_control_version ADD COLUMN approval_comment TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control_version' AND column_name = 'rejected_by') THEN
        ALTER TABLE document_control_version ADD COLUMN rejected_by BIGINT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control_version' AND column_name = 'rejected_at') THEN
        ALTER TABLE document_control_version ADD COLUMN rejected_at TIMESTAMP;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control_version' AND column_name = 'rejection_reason') THEN
        ALTER TABLE document_control_version ADD COLUMN rejection_reason TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control_version' AND column_name = 'effective_date') THEN
        ALTER TABLE document_control_version ADD COLUMN effective_date DATE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'document_control_version' AND column_name = 'review_due_date') THEN
        ALTER TABLE document_control_version ADD COLUMN review_due_date DATE;
    END IF;
END $$;

-- ============================================================
-- 7. New permissions
-- ============================================================
INSERT INTO permission (code, description) VALUES
    ('TASK_CREATE', 'Create scheduled tasks'),
    ('TASK_VIEW', 'View scheduled tasks'),
    ('TASK_EDIT', 'Update scheduled task status'),
    ('INVENTORY_VIEW', 'View reagent inventory'),
    ('INVENTORY_MANAGE', 'Manage reagents and lots'),
    ('INVENTORY_CONSUME', 'Consume reagent lots')
ON CONFLICT (name) DO NOTHING;
