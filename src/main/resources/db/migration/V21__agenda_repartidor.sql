-- V21: Agenda del Repartidor
-- Enriquece la tabla 'rutas' con campos de confirmacion de turno y notas del admin.
-- No se crea tabla nueva: una Ruta con fecha_reparto ya ES una entrada de agenda.

-- Notas del administrador visibles para el repartidor (contexto del recorrido)
ALTER TABLE rutas
    ADD COLUMN IF NOT EXISTS notas_admin TEXT;

-- Confirmacion de disponibilidad del repartidor
-- PENDIENTE: recien asignado, CONFIRMADO: aceptado, RECHAZADO: repartidor rechazo el turno
ALTER TABLE rutas
    ADD COLUMN IF NOT EXISTS confirmacion_repartidor VARCHAR(20)
        DEFAULT 'PENDIENTE'
        CHECK (confirmacion_repartidor IN ('PENDIENTE', 'CONFIRMADO', 'RECHAZADO'));

ALTER TABLE rutas
    ADD COLUMN IF NOT EXISTS fecha_confirmacion TIMESTAMPTZ;

ALTER TABLE rutas
    ADD COLUMN IF NOT EXISTS motivo_rechazo TEXT;

-- Indice para la consulta principal de agenda (repartidor + rango de fechas asc)
CREATE INDEX IF NOT EXISTS idx_rutas_repartidor_fecha
    ON rutas (repartidor_id, fecha_reparto ASC);
