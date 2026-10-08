-- Run before starting the updated music service. Atomic and safe to rerun.
BEGIN;
CREATE TEMP TABLE music_user_id_mapping (
    old_id BIGINT PRIMARY KEY,
    user_uuid UUID NOT NULL UNIQUE
) ON COMMIT DROP;
-- If there are existing user records, add VERIFIED mappings here before running:
-- INSERT INTO music_user_id_mapping VALUES (old_numeric_id, 'existing-auth-user-uuid');
-- Never generate arbitrary UUIDs or assume all old records belong to one user.
CREATE FUNCTION pg_temp.music_user_uuid(old_id BIGINT) RETURNS UUID
LANGUAGE plpgsql AS $$
DECLARE result UUID;
BEGIN
    SELECT user_uuid INTO result FROM music_user_id_mapping WHERE music_user_id_mapping.old_id = $1;
    IF result IS NULL THEN
        RAISE EXCEPTION 'No verified auth UUID mapping for music user %; migration rolled back', old_id;
    END IF;
    RETURN result;
END;
$$;
DO $$
DECLARE target TEXT;
BEGIN
    FOREACH target IN ARRAY ARRAY['music_rating', 'music_favorite', 'music_review'] LOOP
        IF EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = target
              AND column_name = 'user_id' AND data_type = 'bigint') THEN
            EXECUTE format('ALTER TABLE public.%I ALTER COLUMN user_id TYPE UUID USING pg_temp.music_user_uuid(user_id)', target);
        ELSIF NOT EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = target
              AND column_name = 'user_id' AND data_type = 'uuid') THEN
            RAISE EXCEPTION 'Unexpected or missing user_id column in %', target;
        END IF;
    END LOOP;
END;
$$;
COMMIT;
