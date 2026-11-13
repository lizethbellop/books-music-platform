-- Catálogo musical automático. Ejecutar en recommendations_db como propietario.
CREATE TABLE IF NOT EXISTS music_feature_aliases (
    content_type VARCHAR(10) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    canonical_external_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (content_type, external_id),
    FOREIGN KEY (content_type, canonical_external_id)
        REFERENCES music_features(content_type,external_id) ON DELETE CASCADE,
    CHECK (external_id <> canonical_external_id)
);
CREATE TABLE IF NOT EXISTS music_catalog_jobs (
    id BIGSERIAL PRIMARY KEY,
    ref_key VARCHAR(320) NOT NULL,
    content_type VARCHAR(10) NOT NULL CHECK (content_type IN ('ARTIST','ALBUM','SONG')),
    external_id VARCHAR(255),
    musicbrainz_id UUID,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('prepare','expand')),
    feature_version VARCHAR(60) NOT NULL,
    parent_refs TEXT[] NOT NULL DEFAULT '{}',
    metadata JSONB NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(metadata)='object'),
    status VARCHAR(10) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','running','retry','ready','failed')),
    priority INTEGER NOT NULL DEFAULT 0,
    attempts INTEGER NOT NULL DEFAULT 0,
    not_before TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMPTZ,
    error_code VARCHAR(60),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (ref_key,kind,feature_version),
    CHECK (external_id IS NOT NULL OR musicbrainz_id IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS music_catalog_jobs_pending ON music_catalog_jobs(priority DESC,not_before,id)
    WHERE status IN ('pending','running','retry');
CREATE INDEX IF NOT EXISTS music_catalog_jobs_parents ON music_catalog_jobs USING GIN(parent_refs);
INSERT INTO book_provider_budget(provider,budget_day,calls,next_call_at)
VALUES ('musicbrainz',(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,0,CURRENT_TIMESTAMP)
ON CONFLICT (provider) DO NOTHING;
