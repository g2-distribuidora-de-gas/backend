-- V11__cliente_foto_evidencia.sql
-- Migra la foto de evidencia de "por pedido" a "por cliente":
--   * elimina url_foto_evidencia de pedidos (la tabla esta vacia)
--   * agrega foto_evidencia_path a clientes
--   * crea cliente_foto_pendiente para el flujo offline

ALTER TABLE pedidos DROP COLUMN IF EXISTS url_foto_evidencia;

ALTER TABLE clientes ADD COLUMN IF NOT EXISTS foto_evidencia_path VARCHAR(500);

COMMENT ON COLUMN clientes.foto_evidencia_path IS
    'Path en Supabase Storage (bucket privado) de la foto de fachada del cliente. La URL firmada se genera on-the-fly al servir el cliente.';

CREATE TABLE IF NOT EXISTS cliente_foto_pendiente (
    id          BIGSERIAL PRIMARY KEY,
    cliente_id  BIGINT       NOT NULL,
    object_path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cliente_foto_pendiente_cliente
    ON cliente_foto_pendiente(cliente_id);

COMMENT ON TABLE cliente_foto_pendiente IS
    'Imagenes de fachada subidas al storage (path cliente-pending/{id}/...) pero aun no asociadas al cliente. Se limpian con un job diario tras 7 dias.';
