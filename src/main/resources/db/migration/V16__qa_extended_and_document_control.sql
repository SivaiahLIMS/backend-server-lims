/*
  # V16 - OOS/CAPA/Deviation Extended Tables and Document Control

  ## Summary
  This migration extends the QA module with full investigation lifecycle support for OOS/OOT cases,
  CAPA workflows, Deviation tracking, and introduces a new Document Control module.

  ## Changes

  ### 1. Alter existing QA tables
  - `oos_case`: adds case_code, oos_type, phase, assigned_to, assigned_at, root_cause_summary, conclusion
  - `capa`: adds capa_code, title, source_type, source_id, priority, approved_by, approved_at, closure_remarks
  - `deviation`: adds deviation_code, source_type, title, deviation_type, assigned_to, assigned_at, approved_by, approved_at, closure_remarks

  ### 2. New OOS child tables
  - `oos_note`: investigator notes per OOS case
  - `oos_action_item`: corrective action items per OOS case with status lifecycle
  - `oos_audit_trail`: immutable audit log for every state transition on an OOS case

  ### 3. New CAPA child tables
  - `capa_note`: free-text notes per CAPA
  - `capa_action_item`: action items assigned within a CAPA
  - `capa_attachment`: file references attached to a CAPA

  ### 4. New Deviation child tables
  - `deviation_note`: typed notes (OBSERVATION, COMMENT, etc.) per deviation
  - `deviation_action_item`: action items with status lifecycle per deviation
  - `deviation_attachment`: file references per deviation
  - `deviation_audit_trail`: immutable audit log per deviation

  ### 5. New Document Control tables
  - `document_control`: master record for a controlled document (lifecycle: DRAFT → PUBLISHED → OBSOLETE)
  - `document_control_version`: versioned content per controlled document
  - `document_control_attachment`: files attached to a specific document version
  - `document_control_audit`: immutable audit trail per controlled document

  ### Security
  All tables use standard tenant-scoped access via the application layer. No RLS changes.
*/

-- ── OOS Case extensions ───────────────────────────────────────────────────────

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='oos_case' AND column_name='case_code') THEN
        ALTER TABLE oos_case ADD COLUMN case_code VARCHAR(30) UNIQUE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='oos_case' AND column_name='oos_type') THEN
        ALTER TABLE oos_case ADD COLUMN oos_type VARCHAR(10) DEFAULT 'OOS';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='oos_case' AND column_name='phase') THEN
        ALTER TABLE oos_case ADD COLUMN phase VARCHAR(20) DEFAULT 'PHASE_I';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='oos_case' AND column_name='assigned_to') THEN
        ALTER TABLE oos_case ADD COLUMN assigned_to BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='oos_case' AND column_name='assigned_at') THEN
        ALTER TABLE oos_case ADD COLUMN assigned_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='oos_case' AND column_name='root_cause_summary') THEN
        ALTER TABLE oos_case ADD COLUMN root_cause_summary TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='oos_case' AND column_name='conclusion') THEN
        ALTER TABLE oos_case ADD COLUMN conclusion TEXT;
    END IF;
END $$;

-- ── CAPA extensions ───────────────────────────────────────────────────────────

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='capa' AND column_name='capa_code') THEN
        ALTER TABLE capa ADD COLUMN capa_code VARCHAR(30);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='capa' AND column_name='title') THEN
        ALTER TABLE capa ADD COLUMN title VARCHAR(200);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='capa' AND column_name='source_type') THEN
        ALTER TABLE capa ADD COLUMN source_type VARCHAR(30);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='capa' AND column_name='source_id') THEN
        ALTER TABLE capa ADD COLUMN source_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='capa' AND column_name='priority') THEN
        ALTER TABLE capa ADD COLUMN priority VARCHAR(20);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='capa' AND column_name='approved_by') THEN
        ALTER TABLE capa ADD COLUMN approved_by BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='capa' AND column_name='approved_at') THEN
        ALTER TABLE capa ADD COLUMN approved_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='capa' AND column_name='closure_remarks') THEN
        ALTER TABLE capa ADD COLUMN closure_remarks TEXT;
    END IF;
END $$;

-- ── Deviation extensions ──────────────────────────────────────────────────────

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='deviation' AND column_name='deviation_code') THEN
        ALTER TABLE deviation ADD COLUMN deviation_code VARCHAR(30) UNIQUE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='deviation' AND column_name='source_type') THEN
        ALTER TABLE deviation ADD COLUMN source_type VARCHAR(30);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='deviation' AND column_name='title') THEN
        ALTER TABLE deviation ADD COLUMN title VARCHAR(200);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='deviation' AND column_name='deviation_type') THEN
        ALTER TABLE deviation ADD COLUMN deviation_type VARCHAR(20);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='deviation' AND column_name='assigned_to') THEN
        ALTER TABLE deviation ADD COLUMN assigned_to BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='deviation' AND column_name='assigned_at') THEN
        ALTER TABLE deviation ADD COLUMN assigned_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='deviation' AND column_name='approved_by') THEN
        ALTER TABLE deviation ADD COLUMN approved_by BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='deviation' AND column_name='approved_at') THEN
        ALTER TABLE deviation ADD COLUMN approved_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='deviation' AND column_name='closure_remarks') THEN
        ALTER TABLE deviation ADD COLUMN closure_remarks TEXT;
    END IF;
END $$;

-- ── OOS child tables ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS oos_note (
    id          BIGSERIAL PRIMARY KEY,
    oos_case_id BIGINT NOT NULL REFERENCES oos_case(id),
    note_type   VARCHAR(30),
    text        TEXT NOT NULL,
    created_by  BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS oos_action_item (
    id                  BIGSERIAL PRIMARY KEY,
    oos_case_id         BIGINT NOT NULL REFERENCES oos_case(id),
    description         TEXT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assigned_to         BIGINT,
    assigned_at         TIMESTAMP,
    due_date            TIMESTAMP,
    completed_at        TIMESTAMP,
    completion_remarks  TEXT
);

CREATE TABLE IF NOT EXISTS oos_audit_trail (
    id              BIGSERIAL PRIMARY KEY,
    oos_case_id     BIGINT NOT NULL REFERENCES oos_case(id),
    action          VARCHAR(50) NOT NULL,
    old_value_json  TEXT,
    new_value_json  TEXT,
    performed_by    BIGINT,
    performed_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── CAPA child tables ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS capa_note (
    id          BIGSERIAL PRIMARY KEY,
    capa_id     BIGINT NOT NULL REFERENCES capa(id),
    text        TEXT NOT NULL,
    created_by  BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS capa_action_item (
    id                  BIGSERIAL PRIMARY KEY,
    capa_id             BIGINT NOT NULL REFERENCES capa(id),
    description         TEXT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assigned_to         BIGINT,
    assigned_at         TIMESTAMP,
    due_date            TIMESTAMP,
    completed_at        TIMESTAMP,
    completion_remarks  TEXT
);

CREATE TABLE IF NOT EXISTS capa_attachment (
    id           BIGSERIAL PRIMARY KEY,
    capa_id      BIGINT NOT NULL REFERENCES capa(id),
    file_name    VARCHAR(255) NOT NULL,
    file_path    TEXT NOT NULL,
    file_type    VARCHAR(50),
    uploaded_by  BIGINT NOT NULL,
    uploaded_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── Deviation child tables ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS deviation_note (
    id            BIGSERIAL PRIMARY KEY,
    deviation_id  BIGINT NOT NULL REFERENCES deviation(id),
    note_type     VARCHAR(30),
    text          TEXT NOT NULL,
    created_by    BIGINT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS deviation_action_item (
    id                  BIGSERIAL PRIMARY KEY,
    deviation_id        BIGINT NOT NULL REFERENCES deviation(id),
    description         TEXT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    assigned_to         BIGINT,
    assigned_at         TIMESTAMP,
    due_date            TIMESTAMP,
    completed_at        TIMESTAMP,
    completion_remarks  TEXT
);

CREATE TABLE IF NOT EXISTS deviation_attachment (
    id            BIGSERIAL PRIMARY KEY,
    deviation_id  BIGINT NOT NULL REFERENCES deviation(id),
    file_name     VARCHAR(255) NOT NULL,
    file_path     TEXT NOT NULL,
    file_type     VARCHAR(50),
    uploaded_by   BIGINT NOT NULL,
    uploaded_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS deviation_audit_trail (
    id              BIGSERIAL PRIMARY KEY,
    deviation_id    BIGINT NOT NULL REFERENCES deviation(id),
    action          VARCHAR(50) NOT NULL,
    old_value_json  TEXT,
    new_value_json  TEXT,
    performed_by    BIGINT,
    performed_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── Document Control tables ───────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS document_control (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL REFERENCES tenant(id),
    document_code  VARCHAR(50) NOT NULL,
    title          VARCHAR(200) NOT NULL,
    doc_type       VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by     BIGINT NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS document_control_version (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT NOT NULL REFERENCES document_control(id),
    version_number  INT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    content         TEXT,
    reviewed_by     BIGINT,
    reviewed_at     TIMESTAMP,
    approved_by     BIGINT,
    approved_at     TIMESTAMP,
    published_by    BIGINT,
    published_at    TIMESTAMP,
    created_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS document_control_attachment (
    id           BIGSERIAL PRIMARY KEY,
    version_id   BIGINT NOT NULL REFERENCES document_control_version(id),
    file_name    VARCHAR(255) NOT NULL,
    file_path    TEXT NOT NULL,
    file_type    VARCHAR(50),
    uploaded_by  BIGINT NOT NULL,
    uploaded_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS document_control_audit (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT NOT NULL REFERENCES document_control(id),
    action          VARCHAR(50) NOT NULL,
    old_value_json  TEXT,
    new_value_json  TEXT,
    performed_by    BIGINT,
    performed_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── Indexes ───────────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_oos_note_case       ON oos_note(oos_case_id);
CREATE INDEX IF NOT EXISTS idx_oos_action_case     ON oos_action_item(oos_case_id);
CREATE INDEX IF NOT EXISTS idx_oos_audit_case      ON oos_audit_trail(oos_case_id);
CREATE INDEX IF NOT EXISTS idx_capa_note_capa      ON capa_note(capa_id);
CREATE INDEX IF NOT EXISTS idx_capa_action_capa    ON capa_action_item(capa_id);
CREATE INDEX IF NOT EXISTS idx_capa_attach_capa    ON capa_attachment(capa_id);
CREATE INDEX IF NOT EXISTS idx_dev_note_dev        ON deviation_note(deviation_id);
CREATE INDEX IF NOT EXISTS idx_dev_action_dev      ON deviation_action_item(deviation_id);
CREATE INDEX IF NOT EXISTS idx_dev_attach_dev      ON deviation_attachment(deviation_id);
CREATE INDEX IF NOT EXISTS idx_dev_audit_dev       ON deviation_audit_trail(deviation_id);
CREATE INDEX IF NOT EXISTS idx_docctrl_tenant      ON document_control(tenant_id);
CREATE INDEX IF NOT EXISTS idx_docver_document     ON document_control_version(document_id);
CREATE INDEX IF NOT EXISTS idx_docaudit_document   ON document_control_audit(document_id);
