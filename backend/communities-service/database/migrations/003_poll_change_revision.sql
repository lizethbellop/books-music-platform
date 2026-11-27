BEGIN;

ALTER TABLE community_poll_changes
    ADD COLUMN revision UUID NOT NULL DEFAULT gen_random_uuid();

COMMIT;