/*
  # V4 - New Modules Schema

  This migration adds the following new modules to the LIMS platform:

  1. Storage Module
     - `storage_location` - Hierarchical storage locations with reserved zone support
     - `container_storage` - Current placement of chemical containers
     - `container_storage_history` - Movement history of containers
     - `storage_violation` - Storage rule violations tracking

  2. User Skills & Workload
     - `user_skill` - Skills and competency levels per user
     - `user_workload` - Open task counts per user
     - `user_instrument_experience` - Instrument-level experience per user

  3. ELN (Electronic Lab Notebook)
     - `eln_entry` - Analyst notes, observations, attachments per worksheet

  4. Training & Competency
     - `training_material` - Training documents/materials
     - `user_training_record` - Assignment and completion tracking

  5. Unified Task Engine
     - `task_master` - Central task model for all workflow tasks
     - `task_history` - Status change audit trail per task

  6. Chemical Container (new detailed model)
     - `chemical_container` - Individual containers with FEFO support and barcode
     - `chemical_container_reservation` - FEFO-based reservations
     - `document_chemical_consumption` - Consumption records per worksheet

  7. Instrument Enhancements
     - `instrument_reservation` - Instrument booking per worksheet
     - `calibration_task` - Unified calibration task model
     - `instrument_calibration_limit_set` - Versioned calibration limit sets
     - `instrument_reading` - Readings captured during calibration or worksheets

  8. Document & Worksheet Enhancements
     - `document_version` - Separate version lifecycle table (DRAFT → PUBLISHED)
     - `document_test_result` - Test results with OOS/OOT detection fields

  9. Analytics & AI
     - `email_log` - Email dispatch log
     - `instrument_metric_snapshot` - Time-series metrics per instrument
     - `predictive_alert` - AI-generated predictive alerts

  Security: No RLS (PostgreSQL managed by Spring Security at application layer).
  Important: All tables include tenant_id and branch_id for multi-tenant isolation.
*/

-- ==========================================
-- STORAGE MODULE
-- ==========================================

CREATE TABLE IF NOT EXISTS storage_location (
                                                id                BIGSERIAL PRIMARY KEY,
                                                tenant_id         BIGINT NOT NULL REFERENCES tenant(id),
    branch_id         BIGINT NOT NULL REFERENCES branch(id),
    code              VARCHAR(100) NOT NULL,
    name              VARCHAR(255),
    parent_id         BIGINT REFERENCES storage_location(id),
    is_reserved_zone  BOOLEAN NOT NULL DEFAULT FALSE,
    temp_min          DECIMAL(6,2),
    temp_max          DECIMAL(6,2),
    humidity_min      DECIMAL(6,2),
    humidity_max      DECIMAL(6,2),
    capacity          INT,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, branch_id, code)
    );

CREATE TABLE IF NOT EXISTS container_storage (
                                                 id            BIGSERIAL PRIMARY KEY,
                                                 tenant_id     BIGINT NOT NULL REFERENCES tenant(id),
    branch_id     BIGINT NOT NULL REFERENCES branch(id),
    container_id  BIGINT NOT NULL,
    location_id   BIGINT NOT NULL REFERENCES storage_location(id),
    placed_by     BIGINT REFERENCES app_user(id),
    placed_at     TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS container_storage_history (
                                                         id                BIGSERIAL PRIMARY KEY,
                                                         tenant_id         BIGINT NOT NULL REFERENCES tenant(id),
    branch_id         BIGINT NOT NULL REFERENCES branch(id),
    container_id      BIGINT NOT NULL,
    from_location_id  BIGINT REFERENCES storage_location(id),
    to_location_id    BIGINT REFERENCES storage_location(id),
    moved_by          BIGINT REFERENCES app_user(id),
    moved_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    reason            TEXT
    );

CREATE TABLE IF NOT EXISTS storage_violation (
                                                 id            BIGSERIAL PRIMARY KEY,
                                                 tenant_id     BIGINT NOT NULL REFERENCES tenant(id),
    branch_id     BIGINT NOT NULL REFERENCES branch(id),
    container_id  BIGINT,
    location_id   BIGINT REFERENCES storage_location(id),
    description   TEXT,
    violation_type VARCHAR(50),
    status        VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    raised_by     BIGINT REFERENCES app_user(id),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at   TIMESTAMP,
    resolved_by   BIGINT REFERENCES app_user(id)
    );

-- ==========================================
-- USER SKILLS & WORKLOAD
-- ==========================================

CREATE TABLE IF NOT EXISTS user_skill (
                                          id          BIGSERIAL PRIMARY KEY,
                                          tenant_id   BIGINT NOT NULL REFERENCES tenant(id),
    branch_id   BIGINT NOT NULL REFERENCES branch(id),
    user_id     BIGINT NOT NULL REFERENCES app_user(id),
    skill_code  VARCHAR(100) NOT NULL,
    skill_name  VARCHAR(200),
    level       VARCHAR(50),
    verified_by BIGINT REFERENCES app_user(id),
    verified_at TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, user_id, skill_code)
    );

CREATE TABLE IF NOT EXISTS user_workload (
                                             id          BIGSERIAL PRIMARY KEY,
                                             tenant_id   BIGINT NOT NULL REFERENCES tenant(id),
    branch_id   BIGINT NOT NULL REFERENCES branch(id),
    user_id     BIGINT NOT NULL REFERENCES app_user(id),
    open_tasks  INT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, user_id)
    );

CREATE TABLE IF NOT EXISTS user_instrument_experience (
                                                          id             BIGSERIAL PRIMARY KEY,
                                                          tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
    branch_id      BIGINT NOT NULL REFERENCES branch(id),
    user_id        BIGINT NOT NULL REFERENCES app_user(id),
    instrument_id  BIGINT NOT NULL REFERENCES instrument_master(id),
    experience_level VARCHAR(50),
    certified      BOOLEAN NOT NULL DEFAULT FALSE,
    certified_at   TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, user_id, instrument_id)
    );

-- ==========================================
-- ELN MODULE
-- ==========================================

CREATE TABLE IF NOT EXISTS eln_entry (
                                         id                      BIGSERIAL PRIMARY KEY,
                                         tenant_id               BIGINT NOT NULL REFERENCES tenant(id),
    branch_id               BIGINT NOT NULL REFERENCES branch(id),
    worksheet_execution_id  BIGINT REFERENCES worksheet_execution(id),
    title                   VARCHAR(255),
    notes                   TEXT,
    attachments             JSONB,
    tags                    VARCHAR(500),
    created_by              BIGINT REFERENCES app_user(id),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP
    );

-- ==========================================
-- TRAINING & COMPETENCY
-- ==========================================

CREATE TABLE IF NOT EXISTS training_material (
                                                 id           BIGSERIAL PRIMARY KEY,
                                                 tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
    branch_id    BIGINT NOT NULL REFERENCES branch(id),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    file_path    VARCHAR(500),
    version_no   INT NOT NULL DEFAULT 1,
    category     VARCHAR(100),
    duration_mins INT,
    uploaded_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    uploaded_by  BIGINT REFERENCES app_user(id),
    active       BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS user_training_record (
                                                    id           BIGSERIAL PRIMARY KEY,
                                                    tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
    branch_id    BIGINT NOT NULL REFERENCES branch(id),
    user_id      BIGINT NOT NULL REFERENCES app_user(id),
    training_id  BIGINT NOT NULL REFERENCES training_material(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    assigned_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    assigned_by  BIGINT REFERENCES app_user(id),
    completed_at TIMESTAMP,
    approved_by  BIGINT REFERENCES app_user(id),
    score        INT,
    remarks      TEXT,
    UNIQUE (tenant_id, user_id, training_id)
    );

-- ==========================================
-- UNIFIED TASK ENGINE
-- ==========================================

CREATE TABLE IF NOT EXISTS task_master (
                                           id            BIGSERIAL PRIMARY KEY,
                                           tenant_id     BIGINT NOT NULL REFERENCES tenant(id),
    branch_id     BIGINT NOT NULL REFERENCES branch(id),
    type          VARCHAR(50) NOT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    title         VARCHAR(255),
    description   TEXT,
    priority      VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    ref_entity    VARCHAR(50),
    ref_id        BIGINT,
    assignee_id   BIGINT REFERENCES app_user(id),
    created_by    BIGINT REFERENCES app_user(id),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    due_date      TIMESTAMP,
    accepted_at   TIMESTAMP,
    completed_at  TIMESTAMP,
    approved_by   BIGINT REFERENCES app_user(id),
    approved_at   TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS task_history (
                                            id          BIGSERIAL PRIMARY KEY,
                                            tenant_id   BIGINT NOT NULL REFERENCES tenant(id),
    branch_id   BIGINT NOT NULL REFERENCES branch(id),
    task_id     BIGINT NOT NULL REFERENCES task_master(id),
    old_status  VARCHAR(30),
    new_status  VARCHAR(30) NOT NULL,
    changed_by  BIGINT REFERENCES app_user(id),
    changed_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    comment     TEXT
    );

-- ==========================================
-- CHEMICAL CONTAINER (FEFO/BARCODE MODEL)
-- ==========================================

CREATE TABLE IF NOT EXISTS chemical_container (
                                                  id              BIGSERIAL PRIMARY KEY,
                                                  tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    branch_id       BIGINT NOT NULL REFERENCES branch(id),
    chemical_id     BIGINT NOT NULL REFERENCES chemical_master(id),
    registration_id BIGINT REFERENCES chemical_registration(id),
    container_code  VARCHAR(100) NOT NULL,
    quantity        DECIMAL(18,6) NOT NULL DEFAULT 0,
    uom_id          BIGINT REFERENCES uom_details(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    expiry_date     DATE,
    barcode_value   VARCHAR(200),
    lot_no          VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, container_code)
    );

CREATE TABLE IF NOT EXISTS chemical_container_reservation (
                                                              id                      BIGSERIAL PRIMARY KEY,
                                                              tenant_id               BIGINT NOT NULL REFERENCES tenant(id),
    branch_id               BIGINT NOT NULL REFERENCES branch(id),
    container_id            BIGINT NOT NULL REFERENCES chemical_container(id),
    worksheet_execution_id  BIGINT REFERENCES worksheet_execution(id),
    reserved_qty            DECIMAL(18,6) NOT NULL,
    uom_id                  BIGINT REFERENCES uom_details(id),
    status                  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    reserved_by             BIGINT REFERENCES app_user(id),
    reserved_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at              TIMESTAMP,
    converted_at            TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS document_chemical_consumption (
                                                             id                      BIGSERIAL PRIMARY KEY,
                                                             tenant_id               BIGINT NOT NULL REFERENCES tenant(id),
    branch_id               BIGINT NOT NULL REFERENCES branch(id),
    worksheet_execution_id  BIGINT NOT NULL REFERENCES worksheet_execution(id),
    container_id            BIGINT NOT NULL REFERENCES chemical_container(id),
    reservation_id          BIGINT REFERENCES chemical_container_reservation(id),
    consumed_qty            DECIMAL(18,6) NOT NULL,
    uom_id                  BIGINT REFERENCES uom_details(id),
    consumed_by             BIGINT REFERENCES app_user(id),
    consumed_at             TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- ==========================================
-- INSTRUMENT ENHANCEMENTS
-- ==========================================

CREATE TABLE IF NOT EXISTS instrument_reservation (
                                                      id                      BIGSERIAL PRIMARY KEY,
                                                      tenant_id               BIGINT NOT NULL REFERENCES tenant(id),
    branch_id               BIGINT NOT NULL REFERENCES branch(id),
    instrument_id           BIGINT NOT NULL REFERENCES instrument_master(id),
    worksheet_execution_id  BIGINT REFERENCES worksheet_execution(id),
    status                  VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    requested_by            BIGINT REFERENCES app_user(id),
    requested_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_by             BIGINT REFERENCES app_user(id),
    approved_at             TIMESTAMP,
    start_time              TIMESTAMP,
    end_time                TIMESTAMP,
    remarks                 TEXT
    );

CREATE TABLE IF NOT EXISTS instrument_calibration_limit_set (
                                                                id             BIGSERIAL PRIMARY KEY,
                                                                tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
    branch_id      BIGINT NOT NULL REFERENCES branch(id),
    instrument_id  BIGINT NOT NULL REFERENCES instrument_master(id),
    version_no     INT NOT NULL DEFAULT 1,
    method_name    VARCHAR(255),
    limits_json    JSONB NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMP NOT NULL DEFAULT NOW(),
    effective_to   TIMESTAMP,
    uploaded_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by     BIGINT REFERENCES app_user(id),
    UNIQUE (instrument_id, version_no)
    );

CREATE TABLE IF NOT EXISTS calibration_task (
                                                id              BIGSERIAL PRIMARY KEY,
                                                tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    branch_id       BIGINT NOT NULL REFERENCES branch(id),
    instrument_id   BIGINT NOT NULL REFERENCES instrument_master(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    scheduled_at    TIMESTAMP,
    completed_at    TIMESTAMP,
    limit_set_id    BIGINT REFERENCES instrument_calibration_limit_set(id),
    task_id         BIGINT REFERENCES task_master(id),
    created_by      BIGINT REFERENCES app_user(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS instrument_reading (
                                                  id                      BIGSERIAL PRIMARY KEY,
                                                  tenant_id               BIGINT NOT NULL REFERENCES tenant(id),
    branch_id               BIGINT NOT NULL REFERENCES branch(id),
    instrument_id           BIGINT NOT NULL REFERENCES instrument_master(id),
    worksheet_execution_id  BIGINT REFERENCES worksheet_execution(id),
    calibration_task_id     BIGINT REFERENCES calibration_task(id),
    mode                    VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    reading_json            JSONB NOT NULL,
    created_by              BIGINT REFERENCES app_user(id),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- ==========================================
-- DOCUMENT VERSION & TEST RESULTS (OOS/OOT)
-- ==========================================

CREATE TABLE IF NOT EXISTS document_version (
                                                id                BIGSERIAL PRIMARY KEY,
                                                tenant_id         BIGINT NOT NULL REFERENCES tenant(id),
    branch_id         BIGINT NOT NULL REFERENCES branch(id),
    document_id       BIGINT NOT NULL REFERENCES document_master(id),
    version_no        INT NOT NULL,
    lifecycle_state   VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    -- Original filename stored for display
    original_filename VARCHAR(300),
    -- Supabase Storage path: {tenantId}/{documentId}/v{versionNo}/{filename}
    storage_path      VARCHAR(500),
    -- Long-lived signed URL for download/audit access
    file_url          VARCHAR(1000),
    file_size_bytes   BIGINT,
    -- Upload audit
    uploaded_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    uploaded_by       BIGINT REFERENCES app_user(id),
    -- Review audit
    reviewed_by       BIGINT REFERENCES app_user(id),
    reviewed_at       TIMESTAMP,
    review_comment    TEXT,
    -- Approval audit
    approved_by       BIGINT REFERENCES app_user(id),
    approved_at       TIMESTAMP,
    -- Publish audit
    published_at      TIMESTAMP,
    published_by      BIGINT REFERENCES app_user(id),
    -- Retire audit
    retired_at        TIMESTAMP,
    retired_by        BIGINT REFERENCES app_user(id),
    UNIQUE (document_id, version_no)
    );

CREATE TABLE IF NOT EXISTS document_test_result (
                                                    id                        BIGSERIAL PRIMARY KEY,
                                                    tenant_id                 BIGINT NOT NULL REFERENCES tenant(id),
    branch_id                 BIGINT NOT NULL REFERENCES branch(id),
    worksheet_execution_id    BIGINT NOT NULL REFERENCES worksheet_execution(id),
    test_name                 VARCHAR(255) NOT NULL,
    value_numeric             DECIMAL(18,6),
    value_text                VARCHAR(500),
    unit                      VARCHAR(50),
    lower_limit               DECIMAL(18,6),
    upper_limit               DECIMAL(18,6),
    is_oos                    BOOLEAN NOT NULL DEFAULT FALSE,
    is_oot                    BOOLEAN NOT NULL DEFAULT FALSE,
    oos_reason                TEXT,
    oos_detected_at           TIMESTAMP,
    oos_detected_by           BIGINT REFERENCES app_user(id),
    oos_investigation_task_id BIGINT REFERENCES task_master(id),
    created_by                BIGINT REFERENCES app_user(id),
    created_at                TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- ==========================================
-- NOTIFICATIONS (EMAIL LOG)
-- ==========================================

CREATE TABLE IF NOT EXISTS email_log (
                                         id           BIGSERIAL PRIMARY KEY,
                                         tenant_id    BIGINT NOT NULL REFERENCES tenant(id),
    branch_id    BIGINT NOT NULL REFERENCES branch(id),
    to_address   VARCHAR(500) NOT NULL,
    cc_address   VARCHAR(500),
    subject      VARCHAR(500),
    body         TEXT,
    template_key VARCHAR(100),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at      TIMESTAMP,
    error_msg    TEXT,
    ref_entity   VARCHAR(50),
    ref_id       BIGINT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- ==========================================
-- ANALYTICS & PREDICTIVE
-- ==========================================

CREATE TABLE IF NOT EXISTS instrument_metric_snapshot (
                                                          id             BIGSERIAL PRIMARY KEY,
                                                          tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
    branch_id      BIGINT NOT NULL REFERENCES branch(id),
    instrument_id  BIGINT REFERENCES instrument_master(id),
    metric_type    VARCHAR(50),
    metric_value   DECIMAL(18,6),
    metric_date    DATE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS predictive_alert (
                                                id               BIGSERIAL PRIMARY KEY,
                                                tenant_id        BIGINT NOT NULL REFERENCES tenant(id),
    branch_id        BIGINT NOT NULL REFERENCES branch(id),
    entity_type      VARCHAR(50),
    entity_id        BIGINT,
    metric_type      VARCHAR(50),
    predicted_state  VARCHAR(20),
    horizon_days     INT,
    confidence       DECIMAL(5,2),
    details          JSONB,
    status           VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    acknowledged_by  BIGINT REFERENCES app_user(id),
    acknowledged_at  TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- ==========================================
-- ALTER EXISTING TABLES (add missing columns)
-- ==========================================

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'instrument_master' AND column_name = 'barcode_value'
  ) THEN
ALTER TABLE instrument_master ADD COLUMN barcode_value VARCHAR(200);
END IF;
END $$;

-- ==========================================
-- INDEXES
-- ==========================================

CREATE INDEX IF NOT EXISTS idx_storage_location_tenant ON storage_location(tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_container_storage_container ON container_storage(container_id);
CREATE INDEX IF NOT EXISTS idx_container_storage_location ON container_storage(location_id);
CREATE INDEX IF NOT EXISTS idx_task_master_tenant ON task_master(tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_task_master_assignee ON task_master(assignee_id);
CREATE INDEX IF NOT EXISTS idx_task_history_task ON task_history(task_id);
CREATE INDEX IF NOT EXISTS idx_eln_entry_worksheet ON eln_entry(worksheet_execution_id);
CREATE INDEX IF NOT EXISTS idx_chemical_container_tenant ON chemical_container(tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_chemical_container_barcode ON chemical_container(barcode_value);
CREATE INDEX IF NOT EXISTS idx_instrument_reservation_instrument ON instrument_reservation(instrument_id);
CREATE INDEX IF NOT EXISTS idx_document_test_result_worksheet ON document_test_result(worksheet_execution_id);
CREATE INDEX IF NOT EXISTS idx_document_version_document ON document_version(document_id);
CREATE INDEX IF NOT EXISTS idx_predictive_alert_tenant ON predictive_alert(tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_email_log_tenant ON email_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_instrument_metric_snapshot ON instrument_metric_snapshot(instrument_id, metric_date);
