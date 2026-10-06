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