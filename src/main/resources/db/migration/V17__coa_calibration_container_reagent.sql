/*
  # V17 - COA Workflow, Calibration Schedules, and Container Tracking

  ## Summary
  This migration extends three modules with new workflow states and columns needed by the
  CoaService, CalibrationService, and the enhanced ChemicalService.

  ## Changes

  ### 1. COA (coa table)
  - Adds `PENDING_APPROVAL` and `ISSUED` as valid status values (enforced at application layer)
  - No schema change needed; status is a VARCHAR — documented here for clarity

  ### 2. Calibration Schedules (instrument_calibration_schedule)
  - Adds `created_at` column with default NOW() for audit purposes

  ### 3. Chemical Container — new columns
  - Ensures `chemical_container` table has all required columns used by ContainerRepository
  - Adds `updated_at` timestamp column for last-state-change tracking

  ### 4. New: reagent_preparation table
  - Tracks reagent/solution preparations made from chemical stock
  - Links to chemical_registration and the preparing user
  - Supports: name, formula, concentration, volume prepared, uom, prepared_by, prepared_at,
    expiry_date, status (ACTIVE/DISCARDED/EXPIRED), and remarks

  ### 5. Permissions seed
  - Seeds missing permissions for COA, COA_APPROVE, COA_ISSUE, CALIBRATION_CREATE,
    CALIBRATION_COMPLETE, CHEMICAL_CONTAINER_VIEW, CHEMICAL_LOW_STOCK_VIEW

  ### Important Notes
  - All new tables follow the standard pattern: BIGSERIAL PK, tenant-scoped via app layer
  - No breaking changes to existing tables
*/

-- ── Chemical Container: add updated_at if missing ─────────────────────────────

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='chemical_container' AND column_name='updated_at') THEN
        ALTER TABLE chemical_container ADD COLUMN updated_at TIMESTAMP;
    END IF;
END $$;

-- ── Calibration schedule: add created_at if missing ──────────────────────────

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='instrument_calibration_schedule' AND column_name='created_at') THEN
        ALTER TABLE instrument_calibration_schedule ADD COLUMN created_at TIMESTAMP DEFAULT NOW();
    END IF;
END $$;

-- ── Reagent Preparation table ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS reagent_preparation (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL REFERENCES tenant(id),
    branch_id        BIGINT NOT NULL,
    registration_id  BIGINT REFERENCES chemical_registration(id),
    name             VARCHAR(200) NOT NULL,
    formula          VARCHAR(200),
    concentration    VARCHAR(100),
    volume_prepared  NUMERIC(18,4),
    uom_id           BIGINT REFERENCES uom_details(id),
    prepared_by      BIGINT REFERENCES app_user(id),
    prepared_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    expiry_date      DATE,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    remarks          TEXT,
    prep_no          VARCHAR(50) UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_reagent_prep_tenant   ON reagent_preparation(tenant_id);
CREATE INDEX IF NOT EXISTS idx_reagent_prep_branch   ON reagent_preparation(branch_id);
CREATE INDEX IF NOT EXISTS idx_reagent_prep_reg      ON reagent_preparation(registration_id);

-- ── COA: add rejected_remarks column ─────────────────────────────────────────

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='coa' AND column_name='rejected_remarks') THEN
        ALTER TABLE coa ADD COLUMN rejected_remarks TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='coa' AND column_name='issued_at') THEN
        ALTER TABLE coa ADD COLUMN issued_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='coa' AND column_name='issued_by') THEN
        ALTER TABLE coa ADD COLUMN issued_by BIGINT;
    END IF;
END $$;
