-- ============================================================================
-- V2 — Esquema de facturación: facturas, ítems y notas de crédito.
-- Cada documento pertenece a un establecimiento (tenant). El estado refleja
-- el ciclo PENDING -> VALIDATED/REJECTED según la respuesta de Factus/DIAN.
-- ============================================================================

-- ── Facturas ────────────────────────────────────────────────────────────────
CREATE TABLE invoices (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    version                 BIGINT        NOT NULL DEFAULT 0,
    establishment_id        UUID          NOT NULL REFERENCES establishments(id),
    reference_code          VARCHAR(80)   NOT NULL,        -- idempotencia Factus
    number                  VARCHAR(40),                   -- consecutivo DIAN
    cufe                    VARCHAR(255),
    status                  VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING|VALIDATED|REJECTED
    is_validated            BOOLEAN       NOT NULL DEFAULT FALSE,
    validated_at            TIMESTAMP,
    -- snapshot del cliente
    customer_identification VARCHAR(40),
    customer_name           VARCHAR(255),
    customer_email          VARCHAR(180),
    -- importes calculados en el servidor
    subtotal                NUMERIC(15,2) NOT NULL DEFAULT 0,
    tax_total               NUMERIC(15,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(15,2) NOT NULL DEFAULT 0,
    qr_url                  TEXT,
    errors                  TEXT,
    created_at              TIMESTAMP     NOT NULL,
    updated_at              TIMESTAMP     NOT NULL,
    created_by              UUID,
    updated_by              UUID,
    CONSTRAINT uq_invoice_reference UNIQUE (establishment_id, reference_code)
);
CREATE INDEX idx_invoices_est ON invoices(establishment_id);
CREATE INDEX idx_invoices_status ON invoices(status);

-- ── Ítems de factura ────────────────────────────────────────────────────────
CREATE TABLE invoice_items (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    version           BIGINT        NOT NULL DEFAULT 0,
    invoice_id        UUID          NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    code_reference    VARCHAR(80)   NOT NULL,
    name              VARCHAR(255)  NOT NULL,
    quantity          NUMERIC(15,2) NOT NULL,
    price             NUMERIC(15,2) NOT NULL,             -- neto unitario sin impuestos
    discount_rate     NUMERIC(5,2)  NOT NULL DEFAULT 0,
    tax_rate          NUMERIC(5,2)  NOT NULL DEFAULT 0,
    unit_measure_code VARCHAR(10),
    standard_code     VARCHAR(20),
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL,
    created_by        UUID,
    updated_by        UUID
);
CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);

-- ── Notas de crédito (anulan/corrigen una factura validada) ─────────────────
CREATE TABLE credit_notes (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    version                 BIGINT        NOT NULL DEFAULT 0,
    establishment_id        UUID          NOT NULL REFERENCES establishments(id),
    invoice_id              UUID          NOT NULL REFERENCES invoices(id),
    reference_code          VARCHAR(80)   NOT NULL,
    correction_concept_code VARCHAR(10)   NOT NULL,
    number                  VARCHAR(40),
    cude                    VARCHAR(255),
    status                  VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    is_validated            BOOLEAN       NOT NULL DEFAULT FALSE,
    validated_at            TIMESTAMP,
    total                   NUMERIC(15,2) NOT NULL DEFAULT 0,
    errors                  TEXT,
    created_at              TIMESTAMP     NOT NULL,
    updated_at              TIMESTAMP     NOT NULL,
    created_by              UUID,
    updated_by              UUID,
    CONSTRAINT uq_credit_note_reference UNIQUE (establishment_id, reference_code)
);
CREATE INDEX idx_credit_notes_est ON credit_notes(establishment_id);
CREATE INDEX idx_credit_notes_invoice ON credit_notes(invoice_id);
