-- V12__add_version_for_optimistic_locking.sql
-- Agrega columna version para optimistic locking (@Version) en entidades que no la tienen
-- Cliente ya tenia la columna en produccion proveniente de Auditable

ALTER TABLE clientes ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE pedido_detalles ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rutas ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ruta_pedidos ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE garrafas ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN clientes.version IS 'Version para optimistic locking JPA (@Version)';
COMMENT ON COLUMN pedidos.version IS 'Version para optimistic locking JPA (@Version)';
COMMENT ON COLUMN pedido_detalles.version IS 'Version para optimistic locking JPA (@Version)';
COMMENT ON COLUMN rutas.version IS 'Version para optimistic locking JPA (@Version)';
COMMENT ON COLUMN ruta_pedidos.version IS 'Version para optimistic locking JPA (@Version)';
COMMENT ON COLUMN garrafas.version IS 'Version para optimistic locking JPA (@Version)';