CREATE TABLE music_content (
    id BIGSERIAL PRIMARY KEY,
    spotify_id VARCHAR(255) NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    artist_name VARCHAR(255),
    image_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_music_content_spotify_type
        UNIQUE (spotify_id, content_type),

    CONSTRAINT chk_music_content_type
        CHECK (content_type IN ('SONG', 'ALBUM', 'ARTIST'))
);

CREATE TABLE music_rating (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    music_content_id BIGINT NOT NULL,
    rating DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_music_rating_content
        FOREIGN KEY (music_content_id)
        REFERENCES music_content(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_music_rating_user_content
        UNIQUE (user_id, music_content_id)
);

CREATE TABLE music_favorite (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    music_content_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_music_favorite_content
        FOREIGN KEY (music_content_id)
        REFERENCES music_content(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_music_favorite_user_content
        UNIQUE (user_id, music_content_id)
);

CREATE TABLE music_review (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    music_content_id BIGINT NOT NULL,
    review_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_music_review_content
        FOREIGN KEY (music_content_id)
        REFERENCES music_content(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_music_review_user_content
        UNIQUE (user_id, music_content_id)
);

-- No recrear base/usuario. Ejecutar conectado como propietario del servicio.
CREATE TABLE IF NOT EXISTS external_api_budget (
    provider VARCHAR(30) PRIMARY KEY,
    budget_day DATE NOT NULL,
    calls INTEGER NOT NULL DEFAULT 0 CHECK (calls>=0),
    window_start TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    minute_calls INTEGER NOT NULL DEFAULT 0 CHECK (minute_calls>=0),
    blocked_until TIMESTAMPTZ NOT NULL DEFAULT 'epoch'
);
INSERT INTO external_api_budget(provider,budget_day,window_start)
VALUES ('spotify',(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,CURRENT_TIMESTAMP-interval '1 minute')
ON CONFLICT (provider) DO NOTHING;


-- Ejecutar conectado a music_db como propietario; conserva IDs, notas y favoritos.
ALTER TABLE music_content ADD COLUMN IF NOT EXISTS artwork_checked_at TIMESTAMP;
