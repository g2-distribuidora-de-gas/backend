-- ============================================================
-- V18: Módulo de Stock de Garrafas
-- ============================================================


-- ------------------------------------------------------------
-- 3. Tabla: depositos
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS depositos (
    id               BIGSERIAL PRIMARY KEY,
    nombre           VARCHAR(100)  NOT NULL,
    tipo             VARCHAR(50) NOT NULL,
    descripcion      VARCHAR(255),
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    vehiculo_patente VARCHAR(20),
    repartidor_id    BIGINT        REFERENCES usuarios(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    creado_por       VARCHAR(100),
    actualizado_por  VARCHAR(100),
    CONSTRAINT uq_deposito_nombre UNIQUE (nombre)
);

-- ------------------------------------------------------------
-- 4. Tabla: tipos_garrafa_stock
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tipos_garrafa_stock (
    id           BIGSERIAL    PRIMARY KEY,
    codigo       VARCHAR(20)  NOT NULL,
    descripcion  VARCHAR(100) NOT NULL,
    capacidad_kg INTEGER      NOT NULL CHECK (capacidad_kg > 0),
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_tipo_garrafa_stock_codigo UNIQUE (codigo)
);

-- ------------------------------------------------------------
-- 5. Tabla: estados_garrafa
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS estados_garrafa (
    id          BIGSERIAL   PRIMARY KEY,
    codigo      VARCHAR(30) NOT NULL,
    descripcion VARCHAR(100),
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_estado_garrafa_codigo UNIQUE (codigo)
);

-- ------------------------------------------------------------
-- 6. Tabla: stock_garrafa
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_garrafa (
    id               BIGSERIAL PRIMARY KEY,
    deposito_id      BIGINT    NOT NULL REFERENCES depositos(id)          ON DELETE RESTRICT,
    tipo_garrafa_id  BIGINT    NOT NULL REFERENCES tipos_garrafa_stock(id) ON DELETE RESTRICT,
    estado_garrafa_id BIGINT   NOT NULL REFERENCES estados_garrafa(id)    ON DELETE RESTRICT,
    cantidad         INTEGER   NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
    version          BIGINT    NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_deposito_tipo_estado UNIQUE (deposito_id, tipo_garrafa_id, estado_garrafa_id)
);

CREATE INDEX IF NOT EXISTS idx_stock_deposito    ON stock_garrafa (deposito_id);
CREATE INDEX IF NOT EXISTS idx_stock_tipo_estado ON stock_garrafa (tipo_garrafa_id, estado_garrafa_id);

-- ------------------------------------------------------------
-- 7. Tabla: movimientos_garrafa
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movimientos_garrafa (
    id                   BIGSERIAL        PRIMARY KEY,
    tipo_movimiento      VARCHAR(50)  NOT NULL,
    deposito_origen_id   BIGINT           REFERENCES depositos(id)          ON DELETE SET NULL,
    deposito_destino_id  BIGINT           REFERENCES depositos(id)          ON DELETE SET NULL,
    tipo_garrafa_id      BIGINT           NOT NULL REFERENCES tipos_garrafa_stock(id) ON DELETE RESTRICT,
    estado_origen_id     BIGINT           REFERENCES estados_garrafa(id)    ON DELETE SET NULL,
    estado_destino_id    BIGINT           NOT NULL REFERENCES estados_garrafa(id) ON DELETE RESTRICT,
    cantidad             INTEGER          NOT NULL CHECK (cantidad > 0),
    pedido_id            BIGINT           REFERENCES pedidos(id)            ON DELETE SET NULL,
    usuario_id           BIGINT           NOT NULL REFERENCES usuarios(id)  ON DELETE RESTRICT,
    fecha                TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    observaciones        TEXT,
    CONSTRAINT chk_movimiento_tiene_deposito CHECK (
        deposito_origen_id IS NOT NULL OR deposito_destino_id IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_mov_deposito_origen  ON movimientos_garrafa (deposito_origen_id);
CREATE INDEX IF NOT EXISTS idx_mov_deposito_destino ON movimientos_garrafa (deposito_destino_id);
CREATE INDEX IF NOT EXISTS idx_mov_pedido           ON movimientos_garrafa (pedido_id);
CREATE INDEX IF NOT EXISTS idx_mov_usuario          ON movimientos_garrafa (usuario_id);
CREATE INDEX IF NOT EXISTS idx_mov_fecha            ON movimientos_garrafa (fecha DESC);
CREATE INDEX IF NOT EXISTS idx_mov_tipo             ON movimientos_garrafa (tipo_movimiento);

-- ------------------------------------------------------------
-- 8. Datos iniciales: estados de garrafa
-- ------------------------------------------------------------
INSERT INTO estados_garrafa (codigo, descripcion, activo) VALUES
    ('LLENA',          'Garrafa cargada y lista para entregar',    TRUE),
    ('VACIA',          'Garrafa vacía devuelta por el cliente',     TRUE),
    ('RESERVADA',      'Garrafa reservada para un pedido',          TRUE),
    ('REPARACION',     'Garrafa en proceso de reparación',          TRUE),
    ('FUERA_SERVICIO', 'Garrafa fuera de servicio de forma permanente', TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- ------------------------------------------------------------
-- 9. Datos iniciales: tipos de garrafa
-- ------------------------------------------------------------
INSERT INTO tipos_garrafa_stock (codigo, descripcion, capacidad_kg, activo) VALUES
    ('10KG', 'Garrafa 10 Kg', 10, TRUE),
    ('15KG', 'Garrafa 15 Kg', 15, TRUE),
    ('45KG', 'Garrafa 45 Kg', 45, TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- ------------------------------------------------------------
-- 10. Datos iniciales: depósito central por defecto
-- ------------------------------------------------------------
INSERT INTO depositos (nombre, tipo, descripcion, activo) VALUES
    ('Depósito Central', 'DEPOSITO_CENTRAL', 'Depósito principal de la empresa', TRUE)
ON CONFLICT (nombre) DO NOTHING;

