-- Ejecutar en recommendations_db como propietario de la base.
BEGIN;
CREATE TABLE IF NOT EXISTS music_features (
    content_type VARCHAR(10) NOT NULL CHECK (content_type IN ('SONG', 'ALBUM', 'ARTIST')),
    external_id VARCHAR(255) NOT NULL,
    musicbrainz_id UUID NOT NULL,
    features JSONB NOT NULL CHECK (jsonb_typeof(features) = 'object'),
    metadata JSONB NOT NULL CHECK (jsonb_typeof(metadata) = 'object'),
    source_hash VARCHAR(64) NOT NULL CHECK (source_hash ~ '^[0-9a-f]{64}$'),
    source_name VARCHAR(30) NOT NULL CHECK (source_name = 'musicbrainz'),
    feature_version VARCHAR(60) NOT NULL,
    mapping_verified BOOLEAN NOT NULL CHECK (mapping_verified),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (content_type, external_id),
    UNIQUE (content_type, musicbrainz_id)
);
COMMIT;
