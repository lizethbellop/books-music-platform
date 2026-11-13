-- Ejecutar en profile_db como propietario de las tablas.
BEGIN;
ALTER TABLE list_elements DROP CONSTRAINT IF EXISTS chk_list_element_type;
ALTER TABLE list_elements ADD CONSTRAINT chk_list_element_type
    CHECK (element_type IN ('BOOK', 'SONG', 'ALBUM', 'ARTIST'));
ALTER TABLE preference_elements DROP CONSTRAINT IF EXISTS chk_preference_element_type;
ALTER TABLE preference_elements ADD CONSTRAINT chk_preference_element_type
    CHECK (element_type IN ('BOOK', 'SONG', 'ALBUM', 'ARTIST', 'AUTHOR'));
COMMIT;
