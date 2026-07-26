-- V22: Sistema de cobro en punto de entrega
-- Registra el cobro por visita: efectivo, transferencia, mixto, parcial o pendiente.
-- El estado se deriva de los montos (no se persiste como campo editable del cliente).

CREATE TABLE pagos_pedido (
    id                  BIGSERIAL PRIMARY KEY,

    -- Parada a la que corresponde el cobro (1 cobro por parada)
    ruta_pedido_id      BIGINT NOT NULL REFERENCES ruta_pedidos(id),

    -- Montos financieros
    total_pedido        NUMERIC(10,2) NOT NULL,
    monto_efectivo      NUMERIC(10,2) NOT NULL DEFAULT 0,
    monto_transferencia NUMERIC(10,2) NOT NULL DEFAULT 0,
    -- saldo_pendiente = total_pedido - monto_efectivo - monto_transferencia (desnormalizado para queries)
    saldo_pendiente     NUMERIC(10,2) NOT NULL DEFAULT 0,

    -- Estado derivado:
    --   PAGADO   → monto_efectivo + monto_transferencia == total_pedido
    --   PARCIAL  → 0 < suma < total_pedido
    --   PENDIENTE → suma == 0 (aún no cobró nada)
    estado_pago         VARCHAR(20) NOT NULL
                        CHECK (estado_pago IN ('PAGADO', 'PARCIAL', 'PENDIENTE')),

    -- Motivo libre: obligatorio si PENDIENTE (ej: "cliente no disponible", "espera impacto de transfer")
    motivo_pendiente    TEXT,

    -- Repartidor que registró el cobro
    cobrado_por_id      BIGINT REFERENCES usuarios(id),

    -- Auditoría
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    creado_por          VARCHAR(100),
    actualizado_por     VARCHAR(100),

    -- Integridad: solo un registro de cobro por parada
    CONSTRAINT uq_pago_ruta_pedido UNIQUE (ruta_pedido_id),

    -- Integridad financiera: montos no negativos y saldo coherente
    CONSTRAINT chk_montos_positivos
        CHECK (monto_efectivo >= 0 AND monto_transferencia >= 0),
    CONSTRAINT chk_saldo_positivo
        CHECK (saldo_pendiente >= 0),
    CONSTRAINT chk_motivo_pendiente
        CHECK (
            estado_pago != 'PENDIENTE'
            OR (motivo_pendiente IS NOT NULL AND LENGTH(TRIM(motivo_pendiente)) > 0)
        )
);

CREATE INDEX idx_pagos_estado        ON pagos_pedido(estado_pago);
CREATE INDEX idx_pagos_cobrado_por   ON pagos_pedido(cobrado_por_id);
CREATE INDEX idx_pagos_ruta_pedido   ON pagos_pedido(ruta_pedido_id);
