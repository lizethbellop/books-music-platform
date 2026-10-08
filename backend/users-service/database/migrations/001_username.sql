-- Ejecutar en la BD exclusiva del servicio de autenticación antes de iniciarlo.
BEGIN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS username varchar(40);
UPDATE users SET username = 'user_' || replace(id::text, '-', '') WHERE username IS NULL;
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS users_username_unique ON users (username);
CREATE UNIQUE INDEX IF NOT EXISTS users_username_lower_unique ON users (lower(username));
COMMIT;
