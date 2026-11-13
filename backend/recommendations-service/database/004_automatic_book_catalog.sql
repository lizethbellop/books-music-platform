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
