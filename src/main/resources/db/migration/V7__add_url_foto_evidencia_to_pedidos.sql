-- V6__add_url_foto_evidencia_to_pedidos.sql
-- Agrega la columna para almacenar la URL publica de la foto de fachada
-- (evidencia visual) subida a Supabase Storage.

ALTER TABLE pedidos
    ADD COLUMN IF NOT EXISTS url_foto_evidencia VARCHAR(1000);

COMMENT ON COLUMN pedidos.url_foto_evidencia IS
    'URL publica del bucket Supabase Storage con la foto de fachada como evidencia visual del pedido';