/*
  # V6 - Monitoring and Observability Tables

  ## Overview
  Adds two tables to support application observability:

  1. **app_event_log** — Persistent structured log of key business events
     (document uploads, lifecycle transitions, OOS detections, login attempts)
     for compliance audit reporting. This complements the transient Log4j2 log files.

  2. **app_metric_snapshot** — Periodic snapshots of Micrometer metrics persisted
     to the database. Enables trend analysis and dashboards without a Prometheus server.

  ## New Tables

  ### app_event_log
  - `id`            : Primary key
  - `tenant_id`     : Tenant scoping
  - `event_type`    : Category (DOCUMENT, SAMPLE, AUTH, OOS, CALIBRATION, etc.)
  - `action`        : Specific action (UPLOAD, PUBLISH, RETIRE, LOGIN_FAILED, etc.)
  - `entity_type`   : Entity class name
  - `entity_id`     : Entity primary key (string to support all ID types)
  - `user_id`       : User who triggered the event (nullable for system events)
  - `username`      : Captured display name at event time
  - `severity`      : INFO | WARN | ERROR | CRITICAL
  - `message`       : Human-readable summary
  - `metadata`      : JSONB for extra structured context
  - `trace_id`      : Correlates to Log4j2 traceId / GCP trace
  - `occurred_at`   : Event timestamp

  ### app_metric_snapshot
  - `id`            : Primary key
  - `metric_name`   : Micrometer metric name (e.g. lims.documents.uploaded)
  - `metric_value`  : Numeric value at snapshot time
  - `tags`          : JSONB tag/label pairs from Micrometer
  - `snapshotted_at`: When the snapshot was taken

  ## Security
  - RLS disabled on both tables (internal server-to-DB only, never client-facing)
  - App writes via service-role DB user; no Supabase RLS needed
*/

-- ── App Event Log ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS app_event_log (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT REFERENCES tenant(id),
    event_type    VARCHAR(50)  NOT NULL,
    action        VARCHAR(80)  NOT NULL,
    entity_type   VARCHAR(100),
    entity_id     VARCHAR(50),
    user_id       BIGINT       REFERENCES app_user(id),
    username      VARCHAR(100),
    severity      VARCHAR(20)  NOT NULL DEFAULT 'INFO',
    message       TEXT,
    metadata      JSONB,
    trace_id      VARCHAR(64),
    occurred_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ael_tenant_occurred  ON app_event_log(tenant_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ael_event_type       ON app_event_log(event_type, action);
CREATE INDEX IF NOT EXISTS idx_ael_user             ON app_event_log(user_id);
CREATE INDEX IF NOT EXISTS idx_ael_trace            ON app_event_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_ael_severity         ON app_event_log(severity) WHERE severity IN ('WARN','ERROR','CRITICAL');

-- ── App Metric Snapshot ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS app_metric_snapshot (
    id             BIGSERIAL PRIMARY KEY,
    metric_name    VARCHAR(150) NOT NULL,
    metric_value   DOUBLE PRECISION NOT NULL,
    tags           JSONB,
    snapshotted_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ams_name_time ON app_metric_snapshot(metric_name, snapshotted_at DESC);
