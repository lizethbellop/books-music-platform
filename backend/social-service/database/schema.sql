BEGIN;

-- =========================================================
-- SOCIAL SERVICE - MUSA
-- Base de datos: social_db
-- =========================================================


-- =========================================================
-- 1. SEGUIDORES / SEGUIDOS
-- CU-27 Seguir usuario
-- CU-28 Dejar de seguir usuario
-- CU-30 Consultar seguidores y seguidos
-- =========================================================

CREATE TABLE follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    follower_id UUID NOT NULL,
    followed_id UUID NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_follow
        UNIQUE (follower_id, followed_id),

    CONSTRAINT chk_follow_not_self
        CHECK (follower_id <> followed_id)
);


CREATE INDEX idx_follows_follower
    ON follows (follower_id);

CREATE INDEX idx_follows_followed
    ON follows (followed_id);


-- =========================================================
-- 2. PUBLICACIONES
--
-- Puede ser:
-- - publicación escrita manualmente
-- - actividad generada dentro de Musa
-- - publicación asociada a libro/música
-- - publicación perteneciente a comunidad
--
-- NO se almacenan títulos, portadas, nombres de artista,
-- etc. Se guarda únicamente la referencia al servicio dueño.
-- =========================================================

CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    author_id UUID NOT NULL,

    -- Texto opcional escrito por el usuario
    text TEXT,

    -- USER_CREATED = publicación manual
    -- ACTIVITY = generada a partir de una actividad en Musa
    post_origin VARCHAR(20) NOT NULL DEFAULT 'USER_CREATED',

    -- Solo se usa cuando post_origin = ACTIVITY
    activity_type VARCHAR(50),

    -- BOOK, SONG, ALBUM o ARTIST
    content_type VARCHAR(20),

    -- ID externo entendido por Books o Music
    reference_id VARCHAR(255),

    -- Información propia del evento.
    -- Ejemplo:
    -- {"rating": 4.5}
    -- {"status": "LEIDO"}
    activity_data JSONB,

    -- NULL = publicación social normal
    -- UUID = publicación perteneciente a una comunidad
    community_id UUID,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Soft delete
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_post_origin
        CHECK (
            post_origin IN (
                'USER_CREATED',
                'ACTIVITY'
            )
        ),

    CONSTRAINT chk_post_activity_type
        CHECK (
            activity_type IS NULL
            OR activity_type IN (
                'BOOK_REVIEW_CREATED',
                'BOOK_STATUS_CHANGED',
                'BOOK_FAVORITED',

                'MUSIC_RATING_CREATED',
                'MUSIC_REVIEW_CREATED',
                'MUSIC_FAVORITED'
            )
        ),

    CONSTRAINT chk_post_content_type
        CHECK (
            content_type IS NULL
            OR content_type IN (
                'BOOK',
                'SONG',
                'ALBUM',
                'ARTIST'
            )
        ),

    -- content_type y reference_id deben venir juntos
    CONSTRAINT chk_post_content_reference
        CHECK (
            (content_type IS NULL AND reference_id IS NULL)
            OR
            (content_type IS NOT NULL AND reference_id IS NOT NULL)
        ),

    -- Una actividad debe especificar activity_type.
    -- Una publicación manual no debe inventar un activity_type.
    CONSTRAINT chk_post_activity_origin
        CHECK (
            (
                post_origin = 'USER_CREATED'
                AND activity_type IS NULL
            )
            OR
            (
                post_origin = 'ACTIVITY'
                AND activity_type IS NOT NULL
            )
        ),

    -- activity_data solamente tiene sentido en posts de actividad
    CONSTRAINT chk_post_activity_data
        CHECK (
            activity_data IS NULL
            OR post_origin = 'ACTIVITY'
        ),

    -- No permitimos publicaciones completamente vacías.
    CONSTRAINT chk_post_has_content
        CHECK (
            NULLIF(BTRIM(text), '') IS NOT NULL
            OR reference_id IS NOT NULL
            OR activity_type IS NOT NULL
        )
);


-- Feed de publicaciones por usuario
CREATE INDEX idx_posts_author_created
    ON posts (author_id, created_at DESC)
    WHERE deleted_at IS NULL;


-- Publicaciones pertenecientes a una comunidad
CREATE INDEX idx_posts_community_created
    ON posts (community_id, created_at DESC)
    WHERE community_id IS NOT NULL
      AND deleted_at IS NULL;


-- Ayuda al feed general
CREATE INDEX idx_posts_created
    ON posts (created_at DESC)
    WHERE deleted_at IS NULL;


-- =========================================================
-- 3. LIKES
-- CU-32 Dar like
-- CU-33 Quitar like
-- =========================================================

CREATE TABLE post_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    post_id UUID NOT NULL,
    user_id UUID NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_post_likes_post
        FOREIGN KEY (post_id)
        REFERENCES posts(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_post_like
        UNIQUE (post_id, user_id)
);


CREATE INDEX idx_post_likes_post
    ON post_likes (post_id);

CREATE INDEX idx_post_likes_user
    ON post_likes (user_id);


-- =========================================================
-- 4. COMENTARIOS
-- CU-34 Agregar comentario
-- CU-35 Eliminar comentario
--
-- REJECTED no se persiste:
-- si moderación rechaza el comentario, nunca se inserta.
--
-- ACTIVE  = visible
-- REMOVED = eliminado después de haber existido
-- =========================================================

CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    post_id UUID NOT NULL,
    author_id UUID NOT NULL,

    text TEXT NOT NULL,

    status VARCHAR(20)
        NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Usuario que realizó la eliminación.
    -- Puede ser NULL si posteriormente implementamos
    -- una eliminación automática del sistema.
    removed_by UUID,

    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id)
        REFERENCES posts(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_comment_text
        CHECK (
            NULLIF(BTRIM(text), '') IS NOT NULL
        ),

    CONSTRAINT chk_comment_status
        CHECK (
            status IN (
                'ACTIVE',
                'REMOVED'
            )
        ),

    CONSTRAINT chk_comment_removal
        CHECK (
            (
                status = 'ACTIVE'
                AND deleted_at IS NULL
            )
            OR
            (
                status = 'REMOVED'
                AND deleted_at IS NOT NULL
            )
        )
);


CREATE INDEX idx_comments_post_created
    ON comments (post_id, created_at ASC)
    WHERE status = 'ACTIVE';


CREATE INDEX idx_comments_author
    ON comments (author_id);


-- =========================================================
-- 5. TÉRMINOS DE MODERACIÓN
--
-- Catálogo administrable sin recompilar Social.
-- =========================================================

CREATE TABLE moderation_terms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Forma legible
    term VARCHAR(150) NOT NULL,

    -- Forma normalizada utilizada para comparación
    normalized_term VARCHAR(150) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_moderation_term
        UNIQUE (normalized_term)
);


CREATE INDEX idx_moderation_terms_active
    ON moderation_terms (active);


-- =========================================================
-- 6. VARIANTES DE TÉRMINOS
--
-- Ejemplo conceptual:
-- término base -> variantes escritas de diferentes formas.
-- =========================================================

CREATE TABLE moderation_term_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    term_id UUID NOT NULL,

    variant VARCHAR(150) NOT NULL,

    normalized_variant VARCHAR(150) NOT NULL,

    CONSTRAINT fk_moderation_variant_term
        FOREIGN KEY (term_id)
        REFERENCES moderation_terms(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_moderation_variant
        UNIQUE (normalized_variant)
);


CREATE INDEX idx_moderation_variants_term
    ON moderation_term_variants (term_id);


COMMIT;