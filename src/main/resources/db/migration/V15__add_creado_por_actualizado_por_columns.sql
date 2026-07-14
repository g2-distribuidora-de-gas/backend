-- =================================================================
-- V15: Auditoría de usuario (creado_por / actualizado_por)
-- Agrega columnas a tablas que aún no las tienen, alineando el
-- esquema con la entidad Auditable base.
-- =================================================================

ALTER TABLE pedidos         ADD COLUMN IF NOT EXISTS creado_por     VARCHAR(100);
ALTER TABLE pedidos         ADD COLUMN IF NOT EXISTS actualizado_por VARCHAR(100);

ALTER TABLE pedido_detalles ADD COLUMN IF NOT EXISTS creado_por     VARCHAR(100);
ALTER TABLE pedido_detalles ADD COLUMN IF NOT EXISTS actualizado_por VARCHAR(100);

ALTER TABLE usuarios        ADD COLUMN IF NOT EXISTS creado_por     VARCHAR(100);
ALTER TABLE usuarios        ADD COLUMN IF NOT EXISTS actualizado_por VARCHAR(100);

ALTER TABLE garrafas        ADD COLUMN IF NOT EXISTS creado_por     VARCHAR(100);
ALTER TABLE garrafas        ADD COLUMN IF NOT EXISTS actualizado_por VARCHAR(100);

-- Alinear longitud de columnas existentes (clientes, rutas) a 100
ALTER TABLE clientes        ALTER COLUMN creado_por     TYPE VARCHAR(100);
ALTER TABLE clientes        ALTER COLUMN actualizado_por TYPE VARCHAR(100);
ALTER TABLE rutas           ALTER COLUMN creado_por     TYPE VARCHAR(100);
ALTER TABLE rutas           ALTER COLUMN actualizado_por TYPE VARCHAR(100);