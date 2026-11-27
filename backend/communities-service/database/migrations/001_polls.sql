BEGIN;

CREATE TABLE community_polls (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id),
    created_by UUID NOT NULL,
    question VARCHAR(300) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closes_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,

    CONSTRAINT chk_poll_question
        CHECK (length(trim(question)) > 0),

    CONSTRAINT chk_poll_closes_at
        CHECK (closes_at > created_at),

    CONSTRAINT chk_poll_closed_at
        CHECK (closed_at IS NULL OR closed_at >= created_at)
);

CREATE TABLE community_poll_options (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES community_polls(id),
    label VARCHAR(150) NOT NULL,
    position SMALLINT NOT NULL,

    CONSTRAINT chk_poll_option_label
        CHECK (length(trim(label)) > 0),

    CONSTRAINT chk_poll_option_position
        CHECK (position BETWEEN 1 AND 10),

    CONSTRAINT uq_poll_option_position
        UNIQUE (poll_id, position),

    CONSTRAINT uq_poll_option_label
        UNIQUE (poll_id, label),

    CONSTRAINT uq_poll_option_identity
        UNIQUE (poll_id, id)
);

CREATE TABLE community_poll_votes (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES community_polls(id),
    option_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_poll_vote_user
        UNIQUE (poll_id, user_id),

    CONSTRAINT fk_poll_vote_option
        FOREIGN KEY (poll_id, option_id)
        REFERENCES community_poll_options(poll_id, id)
);

CREATE INDEX idx_polls_community_created
    ON community_polls (community_id, created_at DESC, id DESC);

CREATE INDEX idx_poll_votes_results
    ON community_poll_votes (poll_id, option_id);

COMMIT;