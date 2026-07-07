-- V5__add_roles_auth_fields.sql
-- Agrega campos de autenticación y roles a la tabla usuarios

-- Nuevos campos de autenticación y roles
ALTER TABLE usuarios ADD COLUMN rol VARCHAR(30) NOT NULL DEFAULT 'PREVENTISTA';
ALTER TABLE usuarios ADD COLUMN email VARCHAR(150);
ALTER TABLE usuarios ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Índices
CREATE UNIQUE INDEX IF NOT EXISTS idx_usuario_email ON usuarios(email) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_usuario_rol ON usuarios(rol);

-- Comentarios
COMMENT ON COLUMN usuarios.rol IS 'Rol del usuario: PREVENTISTA, REPARTIDOR, ADMIN, SUPER_ADMIN';
COMMENT ON COLUMN usuarios.email IS 'Email único para login. NULL para usuarios legacy sin acceso al sistema';
COMMENT ON COLUMN usuarios.password_hash IS 'BCrypt hash del password';
COMMENT ON COLUMN usuarios.version IS 'Optimistic locking version';
