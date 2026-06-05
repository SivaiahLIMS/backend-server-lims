/*
  # V20 - Reagent Inventory Dispatcher, Scheduler Hooks, and Analytics Extensions

  ## Summary
  This migration seeds new permissions introduced by the reagent inventory dispatcher,
  scheduler hook jobs, and analytics endpoints. No new tables are required — all logic
  uses existing tables (predictive_alert, scheduled_task, inventory_reagent, etc.)

  ## Changes

  ### 1. New Permissions
  - SYSTEM_ADMIN — trigger internal scheduler jobs (run-due, hook triggers)
  - ANALYTICS_VIEW already exists; no duplicate

  ### Security
  - No new tables; RLS enforced at the application layer
*/

INSERT INTO permission (code, description) VALUES
    ('SYSTEM_ADMIN', 'Access to internal system admin operations including scheduler triggers')
    ON CONFLICT (code) DO NOTHING;
