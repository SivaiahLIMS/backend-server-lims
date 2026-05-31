/*
  # V13 - Worksheet Validation Event Log

  ## Overview
  Adds the `worksheet_validation_event` table to record every on-the-fly
  OOS/OOT validation call made during worksheet execution.  Each time a
  user blurs a numeric field the backend validates the entered value against
  the active rule for that field slot and writes one row to this table.

  ## New Tables

  1. `worksheet_validation_event`
     - `id`               — auto-increment primary key
     - `worksheet_id`     — foreign key → worksheet_master.id
     - `slot_id`          — foreign key → document_field_slot.slot_id
     - `value`            — the numeric value that was validated (nullable; null = skipped)
     - `unit`             — unit provided by the caller (nullable)
     - `status`           — validation outcome: PASS | OOT | OOS | NO_RULE
     - `severity`         — HIGH | MEDIUM | LOW | NONE (mirrors status)
     - `message`          — human-readable result message
     - `requires_comment` — whether the UI must prompt for a justification comment
     - `validated_by`     — FK → app_user.id (the analyst who triggered the validation)
     - `validated_at`     — timestamp of the validation call

  ## Indexes
  - `idx_wve_worksheet_id` on worksheet_id for fast per-worksheet event queries
  - `idx_wve_slot_id`      on slot_id for fast per-slot history queries
  - `idx_wve_validated_at` on validated_at for time-range reporting

  ## Notes
  - No RLS; this is a backend-only audit table accessed exclusively via the
    authenticated Spring Boot service layer.
  - The `status` and `severity` columns are stored as plain VARCHAR — no enum
    constraint — so future statuses can be added without a schema migration.
*/

CREATE TABLE IF NOT EXISTS worksheet_validation_event (
    id               BIGSERIAL    PRIMARY KEY,
    worksheet_id     BIGINT       NOT NULL
                       REFERENCES worksheet_master(id) ON DELETE CASCADE,
    slot_id          BIGINT       NOT NULL
                       REFERENCES document_field_slot(slot_id) ON DELETE CASCADE,
    value            NUMERIC(20, 6),
    unit             VARCHAR(50),
    status           VARCHAR(20)  NOT NULL DEFAULT 'NO_RULE',
    severity         VARCHAR(10)  NOT NULL DEFAULT 'NONE',
    message          TEXT,
    requires_comment BOOLEAN      NOT NULL DEFAULT FALSE,
    validated_by     BIGINT       REFERENCES app_user(id) ON DELETE SET NULL,
    validated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wve_worksheet_id ON worksheet_validation_event(worksheet_id);
CREATE INDEX IF NOT EXISTS idx_wve_slot_id      ON worksheet_validation_event(slot_id);
CREATE INDEX IF NOT EXISTS idx_wve_validated_at ON worksheet_validation_event(validated_at DESC);
