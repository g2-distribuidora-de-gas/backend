CREATE TABLE garrafas (
    id               BIGSERIAL PRIMARY KEY,
    tipo             VARCHAR(30)  NOT NULL UNIQUE,
    capacidad_kg     INTEGER      NOT NULL,
    precio           DECIMAL(10,2) NOT NULL,
    stock_disponible INTEGER      NOT NULL DEFAULT 0,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_garrafa_capacidad CHECK (capacidad_kg > 0),
    CONSTRAINT chk_garrafa_precio    CHECK (precio > 0),
    CONSTRAINT chk_garrafa_stock     CHECK (stock_disponible >= 0)
);

CREATE TABLE usuarios (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    apellido   VARCHAR(100) NOT NULL,
    dni        VARCHAR(20)  NOT NULL UNIQUE,
    telefono   VARCHAR(30),
    direccion  VARCHAR(300),
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE pedidos
    ADD CONSTRAINT fk_pedido_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

CREATE INDEX idx_garrafa_tipo   ON garrafas(tipo);
CREATE INDEX idx_usuario_dni    ON usuarios(dni);
