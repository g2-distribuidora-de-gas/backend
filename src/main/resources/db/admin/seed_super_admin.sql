-- Seed Super Admin
-- Ejecutar UNA sola vez para crear el Super Administrador inicial del sistema.
-- 
-- IMPORTANTE: Cambiar el password hash en producción.
-- El hash de abajo corresponde a la contraseña: admin123
-- Generado con BCrypt rounds=10
--
-- Para generar un nuevo hash, usar:
--   Java: new BCryptPasswordEncoder().encode("tu_password_aqui")
--   Online: https://bcrypt-generator.com/

INSERT INTO usuarios (nombre, apellido, dni, email, password_hash, rol, activo, created_at, updated_at)
VALUES (
    'Super',
    'Admin',
    '00000000',
    'superadmin@sistemagas.com',
    '$2a$12$qLxORWN4VwaGJWqxqNgRHuX2R7vaoEUknDagUKjmYuTBUS3JdnknS',
    'SUPER_ADMIN',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (dni) DO NOTHING;
