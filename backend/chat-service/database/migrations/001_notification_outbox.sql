-- Añade únicamente la outbox del productor. No altera mensajes ni seguimientos.
BEGIN;
DO $$
BEGIN
    IF current_database() <> 'chatdb' THEN
        RAISE EXCEPTION 'Base incorrecta para esta migración';
    END IF;
END;
$$;
CREATE TABLE IF NOT EXISTS public.notification_outbox (
    event_id UUID PRIMARY KEY,
    routing_key VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_notification_outbox_pending
    ON public.notification_outbox (next_attempt_at, created_at)
    WHERE published_at IS NULL;
COMMIT;
