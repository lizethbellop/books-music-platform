-- Ejecutar en recommendations_db como propietario de book_features.
BEGIN;
ALTER TABLE book_features
    ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}';
COMMIT;

-- En instalaciones nuevas ejecutar setup.sql y después este archivo.
-- Los registros anteriores seguirán guardados. Prepararlos de nuevo agrega
-- sus metadatos; si la extracción sigue vigente no vuelve a llamar a Groq.
