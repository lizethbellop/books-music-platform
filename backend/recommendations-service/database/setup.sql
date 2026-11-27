-- Completar antes de ejecutar:
-- 1. Reemplazar cada __USUARIO__ por el usuario elegido.
-- 2. Escribir la contraseña entre las comillas de PASSWORD.
-- Ejecutar la instalación con un usuario administrador.

CREATE ROLE __USUARIO__
    WITH LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    PASSWORD '';

CREATE DATABASE recommendations_db
    OWNER __USUARIO__
    ENCODING 'UTF8'
    TEMPLATE template0;

-- 3. Cambiar la conexión a la nueva base.
\connect recommendations_db

-- 4. Crear las tablas como usuario propietario.
SET ROLE __USUARIO__;
BEGIN;

CREATE TABLE book_features (
    work_id VARCHAR(32) PRIMARY KEY,
    features JSONB NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}',
    source_hash VARCHAR(64) NOT NULL,
    extractor_version VARCHAR(100) NOT NULL,
    model_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT book_features_valid_work_id
        CHECK (work_id ~ '^OL[0-9]+W$'),

    CONSTRAINT book_features_features_object
        CHECK (jsonb_typeof(features) = 'object'),

    CONSTRAINT book_features_valid_source_hash
        CHECK (source_hash ~ '^[0-9a-f]{64}$')
);

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

-- Cola pública compartida: no contiene usuarios, contraseñas ni tokens.
CREATE TABLE IF NOT EXISTS book_catalog_jobs (
    id BIGSERIAL PRIMARY KEY,
    work_id VARCHAR(32) NOT NULL CHECK (work_id ~ '^OL[0-9]+W$'),
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('prepare', 'expand')),
    extractor_version VARCHAR(100) NOT NULL,
    model_name VARCHAR(150) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'running', 'retry', 'ready', 'failed')),
    metadata JSONB NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(metadata) = 'object'),
    priority INTEGER NOT NULL DEFAULT 0,
    attempts INTEGER NOT NULL DEFAULT 0,
    not_before TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMPTZ,
    error_code VARCHAR(60),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (work_id, kind, extractor_version, model_name)
);
CREATE INDEX IF NOT EXISTS book_catalog_jobs_pending
    ON book_catalog_jobs (priority DESC, not_before, id)
    WHERE status IN ('pending', 'retry', 'running');

CREATE TABLE IF NOT EXISTS book_provider_budget (
    provider VARCHAR(30) PRIMARY KEY,
    budget_day DATE NOT NULL,
    calls INTEGER NOT NULL DEFAULT 0,
    next_call_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Presupuestos exclusivos de recommendations-service, compartidos por sus procesos.
INSERT INTO book_provider_budget(provider,budget_day,calls,next_call_at)
VALUES ('groq',(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,0,CURRENT_TIMESTAMP),
       ('openlibrary',(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,0,CURRENT_TIMESTAMP),
       ('spotify',(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,0,CURRENT_TIMESTAMP)
ON CONFLICT (provider) DO NOTHING;

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

COMMIT;

RESET ROLE;

-- Instalación nueva completa: libros y música incluidos.
-- Para bases existentes usar las migraciones numeradas; no recrear usuario/base.
