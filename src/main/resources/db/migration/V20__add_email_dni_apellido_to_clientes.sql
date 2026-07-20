ALTER TABLE clientes
    ADD COLUMN apellido VARCHAR(100),
    ADD COLUMN email    VARCHAR(150),
    ADD COLUMN dni      VARCHAR(20);

CREATE UNIQUE INDEX idx_cliente_email ON clientes (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX idx_cliente_dni   ON clientes (dni)   WHERE dni   IS NOT NULL;
