/*
  # V8 - Order Request Module

  Introduces the Order Request workflow used for requesting new chemicals,
  instruments, restocking existing items, and tracking deliveries.

  ## New Tables

  1. `order_request`
     - Covers both CHEMICAL and INSTRUMENT request types
     - Full lifecycle: DRAFT → SUBMITTED → APPROVAL_PENDING → APPROVED → ORDER_PLACED → RECEIVED → CLOSED
     - Links to chemical_master or instrument_master depending on type
     - Tracks supplier, PO number, expected and actual delivery
     - Supports file attachments (path stored as reference)

  2. `order_request_history`
     - Immutable audit trail of every status change
     - Records who changed it, when, and any comment

  ## Permissions
  - Uses existing ORDER_CREATE, ORDER_VIEW, ORDER_EDIT, ORDER_APPROVE, PO_CREATE, PO_APPROVE permissions
  - No new permissions needed

  ## Security
  - RLS enabled on both tables
  - All access scoped to authenticated users' tenant
*/

CREATE TABLE IF NOT EXISTS order_request (
                                             id                    BIGSERIAL PRIMARY KEY,
                                             tenant_id             BIGINT NOT NULL REFERENCES tenant(id),
    branch_id             BIGINT NOT NULL REFERENCES branch(id),
    request_type          VARCHAR(20)  NOT NULL CHECK (request_type IN ('CHEMICAL','INSTRUMENT')),
    chemical_id           BIGINT REFERENCES chemical_master(id),
    instrument_id         BIGINT REFERENCES instrument_master(id),
    quantity              NUMERIC(18,4) NOT NULL DEFAULT 1,
    uom_id                BIGINT REFERENCES uom_details(id),
    reason                TEXT NOT NULL,
    status                VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT','SUBMITTED','APPROVAL_PENDING','APPROVED','ORDER_PLACED','RECEIVED','CLOSED')),
    requested_by          BIGINT NOT NULL REFERENCES app_user(id),
    submitted_at          TIMESTAMP,
    approved_by           BIGINT REFERENCES app_user(id),
    approved_at           TIMESTAMP,
    supplier_id           BIGINT REFERENCES supplier(id),
    expected_delivery_date DATE,
    required_by_date       DATE,
    po_number             VARCHAR(100),
    delivered_quantity    NUMERIC(18,4),
    delivered_at          TIMESTAMP,
    delivery_notes        TEXT,
    attachment_path       VARCHAR(500),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_order_request_tenant_status
    ON order_request(tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_order_request_tenant_branch
    ON order_request(tenant_id, branch_id);

CREATE INDEX IF NOT EXISTS idx_order_request_delivery_date
    ON order_request(tenant_id, expected_delivery_date) WHERE status = 'ORDER_PLACED';

ALTER TABLE order_request ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Tenant members can view own order requests"
    ON order_request FOR SELECT
                                    TO authenticated
                                    USING (
                                    tenant_id IN (
                                    SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint
                                    )
                                    );

CREATE POLICY "Tenant members can insert order requests"
    ON order_request FOR INSERT
    TO authenticated
    WITH CHECK (
        tenant_id IN (
            SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint
        )
    );

CREATE POLICY "Tenant members can update own order requests"
    ON order_request FOR UPDATE
                                           TO authenticated
                                           USING (
                                           tenant_id IN (
                                           SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint
                                           )
                                           )
                         WITH CHECK (
                                           tenant_id IN (
                                           SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint
                                           )
                                           );

-- ─────────────────────────────────────────────
-- Order Request History (immutable audit trail)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_request_history (
                                                     id               BIGSERIAL PRIMARY KEY,
                                                     order_request_id BIGINT NOT NULL REFERENCES order_request(id),
    old_status       VARCHAR(30),
    new_status       VARCHAR(30) NOT NULL,
    changed_by       BIGINT REFERENCES app_user(id),
    changed_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    comment          TEXT
    );

CREATE INDEX IF NOT EXISTS idx_order_request_history_request
    ON order_request_history(order_request_id);

ALTER TABLE order_request_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Tenant members can view order request history"
    ON order_request_history FOR SELECT
                                            TO authenticated
                                            USING (
                                            order_request_id IN (
                                            SELECT id FROM order_request
                                            WHERE tenant_id IN (
                                            SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint
                                            )
                                            )
                                            );

CREATE POLICY "System can insert order request history"
    ON order_request_history FOR INSERT
    TO authenticated
    WITH CHECK (
        order_request_id IN (
            SELECT id FROM order_request
            WHERE tenant_id IN (
                SELECT tenant_id FROM app_user WHERE id = auth.uid()::bigint
            )
        )
    );

-- Seed ORDER_REQUEST permissions
INSERT INTO permission (code, description) VALUES
                                               ('ORDER_REQUEST_VIEW',   'View order requests'),
                                               ('ORDER_REQUEST_CREATE', 'Create order requests'),
                                               ('ORDER_REQUEST_APPROVE','Approve order requests'),
                                               ('ORDER_REQUEST_PLACE',  'Place/convert order request to PO'),
                                               ('ORDER_REQUEST_RECEIVE','Mark order request as received')
    ON CONFLICT (code) DO NOTHING;

-- Grant to relevant roles for SIVAYA tenant
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA'
  AND r.code IN ('SUPER_ADMIN','LAB_MANAGER','INVENTORY_MANAGER','PURCHASER','STOREKEEPER')
  AND p.code IN ('ORDER_REQUEST_VIEW','ORDER_REQUEST_CREATE','ORDER_REQUEST_APPROVE','ORDER_REQUEST_PLACE','ORDER_REQUEST_RECEIVE')
    ON CONFLICT DO NOTHING;

INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA'
  AND r.code IN ('ANALYST','QA_MANAGER','QC_MANAGER','REVIEWER','APPROVER','INSTRUMENT_MANAGER')
  AND p.code IN ('ORDER_REQUEST_VIEW','ORDER_REQUEST_CREATE')
    ON CONFLICT DO NOTHING;

INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT t.id, r.id, p.id
FROM tenant t, role r, permission p
WHERE t.code = 'SIVAYA'
  AND r.code = 'VIEWER'
  AND p.code = 'ORDER_REQUEST_VIEW'
    ON CONFLICT DO NOTHING;
