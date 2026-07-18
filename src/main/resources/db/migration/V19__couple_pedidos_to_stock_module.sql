-- V19__couple_pedidos_to_stock_module.sql
-- Acopla el módulo de pedidos al nuevo sistema de stock (tipos_garrafa_stock)
-- y elimina la dependencia de la tabla deprecada 'garrafas'.

-- 1. Agregar columna de precio a la tabla de stock si no existe
ALTER TABLE tipos_garrafa_stock ADD COLUMN IF NOT EXISTS precio DECIMAL(10,2) NOT NULL DEFAULT 15000.00;

-- 2. Renombrar la columna garrafa_id a tipo_garrafa_id en pedido_detalles
ALTER TABLE pedido_detalles RENAME COLUMN garrafa_id TO tipo_garrafa_id;

-- 3. Reemplazar la Foreign Key para apuntar al nuevo sistema de stock
ALTER TABLE pedido_detalles DROP CONSTRAINT IF EXISTS fk_detalle_garrafa;
ALTER TABLE pedido_detalles ADD CONSTRAINT fk_detalle_tipo_garrafa FOREIGN KEY (tipo_garrafa_id) REFERENCES tipos_garrafa_stock(id);

-- 4. Eliminar la tabla antigua 'garrafas' ya que fue completamente reemplazada
DROP TABLE IF EXISTS garrafas CASCADE;
