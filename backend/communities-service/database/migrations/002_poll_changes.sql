BEGIN;

CREATE TABLE community_poll_changes (
    poll_id UUID PRIMARY KEY REFERENCES community_polls(id),

    changed_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),

    attempts INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_poll_changes_attempts
        CHECK (attempts BETWEEN 0 AND 1000)
);

CREATE INDEX idx_poll_changes_pending
    ON community_poll_changes (next_attempt_at, poll_id);

COMMIT;