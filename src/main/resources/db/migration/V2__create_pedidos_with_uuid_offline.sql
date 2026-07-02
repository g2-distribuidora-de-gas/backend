-- V2__create_pedidos_with_uuid_offline.sql
-- Crea tabla pedidos (cabecera) y pedido_detalles (lineas con soporte offline)
-- NO incluye FKs hacia usuarios/garrafas porque esas tablas las crea Dev 2 en V3.
-- Dev 2 agregara las FKs en su migracion (ej. ALTER TABLE pedidos ADD CONSTRAINT fk_pedido_usuario FOREIGN KEY...)

CREATE TABLE pedidos (
    id                BIGSERIAL PRIMARY KEY,
    uuid_offline      VARCHAR(100) UNIQUE,
    usuario_id        BIGINT       NOT NULL,
    direccion_entrega VARCHAR(300) NOT NULL,
    estado            VARCHAR(30)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pedido_uuid_offline ON pedidos(uuid_offline);
CREATE INDEX idx_pedido_estado       ON pedidos(estado);
CREATE INDEX idx_pedido_usuario      ON pedidos(usuario_id);

CREATE TABLE pedido_detalles (
    id               BIGSERIAL PRIMARY KEY,
    pedido_id        BIGINT        NOT NULL,
    garrafa_id       BIGINT        NOT NULL,
    cantidad         INTEGER       NOT NULL,
    precio_unitario  DECIMAL(10,2) NOT NULL,
    subtotal         DECIMAL(10,2) NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_detalle_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT chk_detalle_cantidad        CHECK (cantidad > 0),
    CONSTRAINT chk_detalle_precio_unitario CHECK (precio_unitario > 0),
    CONSTRAINT chk_detalle_subtotal        CHECK (subtotal > 0)
);

CREATE INDEX idx_detalle_pedido  ON pedido_detalles(pedido_id);
CREATE INDEX idx_detalle_garrafa ON pedido_detalles(garrafa_id);

COMMENT ON TABLE  pedido_detalles IS 'Lineas de cada pedido. Un pedido puede tener N detalles de distintas garrafas';
COMMENT ON COLUMN pedido_detalles.precio_unitario IS 'Precio snapshot de la garrafa al momento del pedido';
COMMENT ON COLUMN pedido_detalles.subtotal        IS 'cantidad * precio_unitario, calculado al crear';