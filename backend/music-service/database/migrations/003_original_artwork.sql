-- Ejecutar conectado a music_db como propietario; conserva IDs, notas y favoritos.
ALTER TABLE music_content ADD COLUMN IF NOT EXISTS artwork_checked_at TIMESTAMP;
