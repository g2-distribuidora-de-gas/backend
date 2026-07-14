ALTER TABLE clientes ADD COLUMN uuid_offline VARCHAR(100) UNIQUE;
CREATE INDEX idx_cliente_uuid_offline ON clientes (uuid_offline);
