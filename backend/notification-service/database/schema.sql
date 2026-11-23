-- MUSA / notification-service. PostgreSQL 13+.
-- Instalación nueva: ejecutar conectado a notifications_db como notification_user.
-- No crea la base ni roles. No inserta notificaciones ni modifica datos existentes.
-- IF NOT EXISTS permite repetirlo sobre este esquema, pero no migra otros esquemas.
BEGIN;

DO $$
BEGIN
    IF current_database() <> 'notifications_db' THEN
        RAISE EXCEPTION 'Conéctate a notifications_db antes de ejecutar este esquema';
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(300) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMPTZ,
    CONSTRAINT notifications_event_recipient_unique UNIQUE (event_id, recipient_id),
    CONSTRAINT notifications_type_check CHECK (
        type IN ('CHAT_MESSAGE_CREATED', 'USER_FOLLOWED', 'POST_LIKED',
                 'POST_COMMENTED', 'POST_PUBLISHED', 'GROUP_MEMBER_ADDED', 'GROUP_MEMBER_REMOVED', 'GROUP_ADMIN_CHANGED')
    ),
    CONSTRAINT notifications_target_type_check CHECK (
        target_type IN ('CONVERSATION', 'PROFILE', 'POST')
    )
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
    ON public.notifications (recipient_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_unread
    ON public.notifications (recipient_id)
    WHERE read_at IS NULL;

COMMIT;
