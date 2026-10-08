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