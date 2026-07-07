-- =================================================================
-- V6: Esquema de Geolocalización, Rutas y Clientes
-- =================================================================

-- 1. Crear tabla de clientes
CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(30),
    direccion VARCHAR(300) NOT NULL,
    latitud DECIMAL(10,7),
    longitud DECIMAL(10,7),
    place_id VARCHAR(100),
    geocode_precision VARCHAR(50),
    geo_actualizado_en TIMESTAMPTZ,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por VARCHAR(50),
    actualizado_por VARCHAR(50)
);

-- 2. Modificar la tabla de pedidos
-- Eliminamos usuario_id porque ahora los pedidos van atados a un cliente
ALTER TABLE pedidos DROP COLUMN usuario_id;
ALTER TABLE pedidos ADD COLUMN cliente_id BIGINT NOT NULL;
ALTER TABLE pedidos ADD CONSTRAINT fk_pedidos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id);

-- 3. Crear tabla de Rutas
CREATE TABLE rutas (
    id BIGSERIAL PRIMARY KEY,
    fecha_reparto DATE NOT NULL,
    repartidor_id BIGINT NOT NULL,
    origen_lat DECIMAL(10,7),
    origen_lng DECIMAL(10,7),
    distancia_total_m INTEGER,
    duracion_total_s INTEGER,
    geometria TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'PLANIFICADA',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creado_por VARCHAR(50),
    actualizado_por VARCHAR(50),
    CONSTRAINT fk_rutas_repartidor FOREIGN KEY (repartidor_id) REFERENCES usuarios(id)
);

-- 4. Crear tabla intermedia de Ruta y Pedidos
CREATE TABLE ruta_pedidos (
    id BIGSERIAL PRIMARY KEY,
    ruta_id BIGINT NOT NULL,
    pedido_id BIGINT NOT NULL,
    orden INTEGER NOT NULL,
    distancia_desde_anterior_m INTEGER,
    duracion_desde_anterior_s INTEGER,
    hora_estimada_llegada TIMESTAMPTZ,
    estado_entrega VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    CONSTRAINT fk_ruta_pedidos_ruta FOREIGN KEY (ruta_id) REFERENCES rutas(id) ON DELETE CASCADE,
    CONSTRAINT fk_ruta_pedidos_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    UNIQUE (ruta_id, pedido_id)
);
