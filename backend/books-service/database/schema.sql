CREATE TABLE book (
    id UUID PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    isbn VARCHAR(30),
    cover_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_book (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    book_id UUID NOT NULL,
    reading_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_book_book
        FOREIGN KEY (book_id)
        REFERENCES book(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_user_book
        UNIQUE (user_id, book_id),

    CONSTRAINT chk_reading_status
        CHECK (reading_status IN ('POR_LEER', 'LEYENDO', 'LEIDO'))
);

CREATE TABLE review (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    book_id UUID NOT NULL,
    rating NUMERIC(2,1) NOT NULL,
    review_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_review_book
        FOREIGN KEY (book_id)
        REFERENCES book(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_review_user_book
        UNIQUE (user_id, book_id),

    CONSTRAINT chk_rating_range
        CHECK (
            rating >= 0.5
            AND rating <= 5.0
            AND MOD(rating * 10, 5) = 0
        )
);

CREATE TABLE favorite_book (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    book_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_favorite_book
        FOREIGN KEY (book_id)
        REFERENCES book(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_favorite_book
        UNIQUE (user_id, book_id)
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
VALUES ('openlibrary',(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,CURRENT_TIMESTAMP-interval '1 minute')
ON CONFLICT (provider) DO NOTHING;
