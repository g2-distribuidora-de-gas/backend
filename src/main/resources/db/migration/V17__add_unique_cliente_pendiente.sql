-- V17: Garantiza como maximo UNA imagen pendiente por cliente.
-- Soluciona la carrera entre dos subidas simultaneas del mismo cliente
-- y permite reintento seguro desde el cliente offline.
ALTER TABLE cliente_foto_pendiente
    DROP CONSTRAINT IF EXISTS uk_cliente_foto_pendiente;

ALTER TABLE cliente_foto_pendiente
    ADD CONSTRAINT uk_cliente_foto_pendiente UNIQUE (cliente_id);
