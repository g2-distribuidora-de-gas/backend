ALTER TABLE pedidos
ADD COLUMN creador_id BIGINT;

ALTER TABLE pedidos
ADD CONSTRAINT fk_pedido_creador
FOREIGN KEY (creador_id) REFERENCES usuarios(id);

CREATE INDEX idx_pedido_creador_id ON pedidos(creador_id);
