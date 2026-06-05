/*
  # V13 - Worksheet Validation Event Log

  ## Overview
  Adds the `worksheet_validation_event` table to record every on-the-fly
  OOS/OOT validation call made during worksheet execution. Each time a
  user blurs a numeric field the backend validates the entered value against
  the active rule for that field slot and writes one row to this table.
  Also records post-compute validation events (source = COMPUTED_RESULT).

  ## New Tables

  1. `worksheet_validation_event`
     - `id`               — auto-increment primary key
     - `worksheet_id`     — worksheet reference (plain BIGINT, no FK)
     - `slot_id`          — field slot reference (plain BIGINT, no FK)
     - `value`            — the numeric value that was validated
     - `unit`             — unit provided by the caller
     - `status`           — PASS | OOT | OOS | NO_RULE
     - `severity`         — HIGH | MEDIUM | LOW | NONE
     - `message`          — human-readable result message
     - `requires_comment` — whether the UI must prompt for a justification comment
     - `requires_review`  — whether OOT result requires supervisor review
     - `requires_investigation` — whether OOS result requires an investigation record
     - `requires_capa`    — whether OOS result requires a CAPA record
     - `source`           — FIELD_BLUR | COMPUTED_RESULT
     - `validated_by`     — user ID who triggered the validation
     - `validated_at`     — timestamp of the validation call
*/

CREATE TABLE IF NOT EXISTS worksheet_validation_event (
                                                          id               BIGSERIAL    PRIMARY KEY,
                                                          worksheet_id     BIGINT       NOT NULL,
                                                          slot_id          BIGINT       NOT NULL,
                                                          value            NUMERIC(20, 6),
    unit             VARCHAR(50),
    status           VARCHAR(20)  NOT NULL DEFAULT 'NO_RULE',
    severity         VARCHAR(10)  NOT NULL DEFAULT 'NONE',
    message          TEXT,
    requires_comment      BOOLEAN NOT NULL DEFAULT FALSE,
    requires_review       BOOLEAN NOT NULL DEFAULT FALSE,
    requires_investigation BOOLEAN NOT NULL DEFAULT FALSE,
    requires_capa         BOOLEAN NOT NULL DEFAULT FALSE,
    source           VARCHAR(30)  NOT NULL DEFAULT 'FIELD_BLUR',
    validated_by     BIGINT,
    validated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_wve_worksheet_id ON worksheet_validation_event(worksheet_id);
CREATE INDEX IF NOT EXISTS idx_wve_slot_id      ON worksheet_validation_event(slot_id);
CREATE INDEX IF NOT EXISTS idx_wve_validated_at ON worksheet_validation_event(validated_at DESC);
CREATE INDEX IF NOT EXISTS idx_wve_source       ON worksheet_validation_event(source);
