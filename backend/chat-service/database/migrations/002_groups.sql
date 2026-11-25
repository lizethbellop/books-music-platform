-- Historial y autores existentes se conservan. No se eliminan participantes.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'chatdb' THEN RAISE EXCEPTION 'Conéctate a chatdb'; END IF;
END $$;
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS name VARCHAR(100);
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS owner_id UUID;
ALTER TABLE public.conversations ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ;
ALTER TABLE public.conversation_participants ADD COLUMN IF NOT EXISTS left_at TIMESTAMPTZ;
-- Compatibilidad con grupos creados manualmente antes de esta implementación.
UPDATE public.conversations SET name = 'Grupo' WHERE type = 'GROUP' AND (name IS NULL OR btrim(name) = '');
UPDATE public.conversations c SET owner_id = (
    SELECT p.user_id FROM public.conversation_participants p WHERE p.conversation_id = c.id AND p.left_at IS NULL
    ORDER BY p.joined_at, p.user_id LIMIT 1)
WHERE c.type = 'GROUP' AND c.owner_id IS NULL;
UPDATE public.conversations SET closed_at = CURRENT_TIMESTAMP WHERE type = 'GROUP' AND owner_id IS NULL AND closed_at IS NULL;
ALTER TABLE public.conversations DROP CONSTRAINT IF EXISTS conversations_group_metadata_check;
ALTER TABLE public.conversations ADD CONSTRAINT conversations_group_metadata_check CHECK (
    (type = 'PRIVATE' AND name IS NULL AND owner_id IS NULL AND closed_at IS NULL)
    OR (type = 'GROUP' AND name IS NOT NULL AND name ~ '[^[:space:]]' AND (owner_id IS NOT NULL OR closed_at IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_conversation_participants_active_user
    ON public.conversation_participants(user_id, conversation_id) WHERE left_at IS NULL;
COMMIT;
