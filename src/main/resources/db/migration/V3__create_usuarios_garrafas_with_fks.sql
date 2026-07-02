-- V3__create_usuarios_garrafas_with_fks.sql
-- Crea tablas garrafas y usuarios, y agrega las FKs que V2 no pudo
-- declarar (porque dependia de estas tablas).

CREATE TABLE IF NOT EXISTS garrafas (
    id               BIGSERIAL PRIMARY KEY,
    tipo             VARCHAR(30)  NOT NULL UNIQUE,
    capacidad_kg     INTEGER      NOT NULL,
    precio           DECIMAL(10,2) NOT NULL,
    stock_disponible INTEGER      NOT NULL DEFAULT 0,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_garrafa_capacidad CHECK (capacidad_kg > 0),
    CONSTRAINT chk_garrafa_precio    CHECK (precio > 0),
    CONSTRAINT chk_garrafa_stock     CHECK (stock_disponible >= 0)
);

CREATE TABLE IF NOT EXISTS usuarios (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    apellido   VARCHAR(100) NOT NULL,
    dni        VARCHAR(20)  NOT NULL UNIQUE,
    telefono   VARCHAR(30),
    direccion  VARCHAR(300),
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Agregar FKs solo si no existen (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_pedido_usuario'
    ) THEN
        ALTER TABLE pedidos
            ADD CONSTRAINT fk_pedido_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_detalle_garrafa'
    ) THEN
        ALTER TABLE pedido_detalles
            ADD CONSTRAINT fk_detalle_garrafa FOREIGN KEY (garrafa_id) REFERENCES garrafas(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_garrafa_tipo   ON garrafas(tipo);
CREATE INDEX IF NOT EXISTS idx_usuario_dni    ON usuarios(dni);
