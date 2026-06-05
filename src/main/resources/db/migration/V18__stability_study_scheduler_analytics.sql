/*
  # V18 - Stability Study, Scheduler Job Log, and Entity Extensions

  ## Summary
  This migration adds the Stability Study module (studies, timepoints, results),
  a scheduler job execution log table, and extends PredictiveAlert and CalibrationTask
  with new columns needed by the enhanced Scheduler and Analytics services.

  ## Changes

  ### 1. New Table: stability_study
  - Master record for a stability study linked to a product
  - studyType: REAL_TIME / ACCELERATED / INTERMEDIATE
  - status: DRAFT / ACTIVE / COMPLETED / DISCONTINUED
  - auto-generated studyCode (STB-YYYY-XXXXX)

  ### 2. New Table: stability_study_timepoint
  - One row per analysis timepoint (T0, T3M, T6M, T12M, etc.)
  - Tracks scheduled vs. completed dates
  - status: PENDING / COMPLETED

  ### 3. New Table: stability_study_result
  - Individual test parameter results per timepoint
  - Captures parameter, specification, actual result, pass/fail determination

  ### 4. New Table: scheduled_job_log
  - Execution history for all @Scheduled background jobs
  - Tracks job name, start/end time, status (RUNNING/SUCCESS/FAILED), records processed

  ### 5. Alter Table: predictive_alert
  - Adds alert_type, message, severity columns used by SchedulerService

  ### 6. Alter Table: calibration_task
  - Adds schedule_id FK, task_type, due_date columns used by SchedulerService

  ### 7. Permissions seed
  - Seeds STABILITY_VIEW, STABILITY_CREATE, STABILITY_MANAGE permissions

  ### Security
  - All new tables use standard tenant-scoped access via the application layer
  - No additional RLS changes needed

  ### Important Notes
  - All tables use BIGSERIAL primary keys and IF NOT EXISTS guards
  - Indexes added on FK columns for query performance
*/

-- ── Stability Study ───────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS stability_study (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL REFERENCES tenant(id),
    branch_id         BIGINT NOT NULL,
    study_code        VARCHAR(50) UNIQUE,
    product_id        BIGINT REFERENCES product_master(product_id),
    title             VARCHAR(200) NOT NULL,
    study_type        VARCHAR(30) NOT NULL,
    protocol          TEXT,
    storage_condition VARCHAR(200),
    start_date        DATE,
    end_date          DATE,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by        BIGINT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stability_tenant     ON stability_study(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stability_branch     ON stability_study(branch_id);
CREATE INDEX IF NOT EXISTS idx_stability_product    ON stability_study(product_id);

-- ── Stability Study Timepoint ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS stability_study_timepoint (
    id              BIGSERIAL PRIMARY KEY,
    study_id        BIGINT NOT NULL REFERENCES stability_study(id),
    timepoint       VARCHAR(30) NOT NULL,
    scheduled_date  DATE,
    completed_date  DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_by    BIGINT
);

CREATE INDEX IF NOT EXISTS idx_stability_tp_study  ON stability_study_timepoint(study_id);

-- ── Stability Study Result ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS stability_study_result (
    id              BIGSERIAL PRIMARY KEY,
    timepoint_id    BIGINT NOT NULL REFERENCES stability_study_timepoint(id),
    parameter       VARCHAR(200) NOT NULL,
    specification   VARCHAR(200),
    result          VARCHAR(200),
    pass_fail       VARCHAR(10),
    tested_by       BIGINT,
    tested_at       TIMESTAMP,
    remarks         TEXT
);

CREATE INDEX IF NOT EXISTS idx_stability_result_tp ON stability_study_result(timepoint_id);

-- ── Scheduled Job Log ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS scheduled_job_log (
    id                  BIGSERIAL PRIMARY KEY,
    job_name            VARCHAR(100) NOT NULL,
    started_at          TIMESTAMP NOT NULL,
    completed_at        TIMESTAMP,
    status              VARCHAR(20) NOT NULL,
    records_processed   INT,
    message             TEXT
);

CREATE INDEX IF NOT EXISTS idx_job_log_name ON scheduled_job_log(job_name);

-- ── PredictiveAlert: extend with alert_type, message, severity ────────────────

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='predictive_alert' AND column_name='alert_type') THEN
        ALTER TABLE predictive_alert ADD COLUMN alert_type VARCHAR(50);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='predictive_alert' AND column_name='message') THEN
        ALTER TABLE predictive_alert ADD COLUMN message TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='predictive_alert' AND column_name='severity') THEN
        ALTER TABLE predictive_alert ADD COLUMN severity VARCHAR(20);
    END IF;
END $$;

-- ── CalibrationTask: add schedule_id, task_type, due_date ────────────────────

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='calibration_task' AND column_name='schedule_id') THEN
        ALTER TABLE calibration_task ADD COLUMN schedule_id BIGINT REFERENCES instrument_calibration_schedule(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='calibration_task' AND column_name='task_type') THEN
        ALTER TABLE calibration_task ADD COLUMN task_type VARCHAR(30);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='calibration_task' AND column_name='due_date') THEN
        ALTER TABLE calibration_task ADD COLUMN due_date DATE;
    END IF;
END $$;

-- ── Permissions seed for new modules ─────────────────────────────────────────

INSERT INTO permission (code, description) VALUES
    ('STABILITY_VIEW',     'View stability studies and results'),
    ('STABILITY_CREATE',   'Create new stability studies'),
    ('STABILITY_MANAGE',   'Activate, complete, or discontinue stability studies and manage timepoints/results')
ON CONFLICT (name) DO NOTHING;
