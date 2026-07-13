-- =============================================================
-- TRUNCATE_SCHEMA.SQL
-- =============================================================
-- Vacía todas las tablas de la aplicación en el esquema public,
-- reiniciando los IDs (auto-incrementales), pero MANTIENE la
-- estructura de las tablas y el historial de Flyway.
-- =============================================================

BEGIN;

TRUNCATE TABLE 
    usuarios,
    garrafas,
    clientes,
    pedidos,
    pedido_detalles,
    rutas,
    ruta_pedidos
RESTART IDENTITY CASCADE;

COMMIT;

-- Mensaje de confirmacion
SELECT 'Todas las tablas han sido vaciadas (truncadas) con éxito.' AS resultado;
