-- Agregar columna para url_comprobante
ALTER TABLE pagos_pedido ADD COLUMN url_comprobante VARCHAR(255) DEFAULT NULL;
