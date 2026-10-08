-- Ejecutar después de database/schema.sql en una instalación nueva.
-- En una BD existente, ejecutar después de la migración que corresponda.
-- Roles que ya utiliza el formulario de registro Flutter.
INSERT INTO roles (name)
VALUES ('USUARIO'), ('VENDEDOR')
ON CONFLICT (name) DO NOTHING;
