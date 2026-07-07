-- =============================================================
-- RESET_SCHEMA.SQL
-- =============================================================
-- BORRA todas las tablas del esquema public de Supabase.
-- Ejecutar UNA SOLA VEZ antes del primer deploy con Flyway.
--
-- Como usarlo:
--   1. Desde PowerShell:    .\scripts\reset-db.ps1
--   2. Desde bash:          ./scripts/reset-db.sh
--   3. Manual con psql:     psql "$SUPABASE_DB_URL" -f db/admin/reset_schema.sql
--   4. Manual desde Supabase SQL Editor: pegar y ejecutar
--
-- ATENCION: este script BORRA TODOS LOS DATOS. Solo para setup
-- inicial o para un reset completo. NO usar en produccion con
-- datos reales.
-- =============================================================

BEGIN;

-- Borrar tablas en orden inverso por las FKs
DROP TABLE IF EXISTS ruta_pedidos CASCADE;
DROP TABLE IF EXISTS rutas          CASCADE;
DROP TABLE IF EXISTS pedido_detalles CASCADE;
DROP TABLE IF EXISTS pedidos        CASCADE;
DROP TABLE IF EXISTS clientes       CASCADE;
DROP TABLE IF EXISTS garrafas       CASCADE;
DROP TABLE IF EXISTS usuarios       CASCADE;

-- (Los indices se borcan automaticamente al hacer DROP TABLE CASCADE)

-- Borrar la tabla de historial de Flyway para que pueda volver a crear todo
DROP TABLE IF EXISTS flyway_schema_history CASCADE;

COMMIT;

-- Confirmacion
SELECT
    'Tablas restantes en public:' AS mensaje,
    count(*) AS cantidad
FROM information_schema.tables
WHERE table_schema = 'public';
