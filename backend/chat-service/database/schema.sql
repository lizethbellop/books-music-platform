-- MUSA / chat-service: esquema inicial para PostgreSQL 13+.
-- Ejecutar en chatdb con psql -X -v ON_ERROR_STOP=1 -f database/schema.sql
-- No crea la BD, usuarios ni datos de prueba. No es una migración.
BEGIN;

DO $$
BEGIN
    IF current_database() <> 'chatdb' THEN
        RAISE EXCEPTION 'Conéctate a chatdb antes de aplicar este esquema';
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS public.conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(10) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT conversations_type_check CHECK (type IN ('PRIVATE', 'GROUP'))
);

CREATE TABLE IF NOT EXISTS public.conversation_participants (
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT conversation_participants_pkey PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT conversation_participants_conversation_fk
        FOREIGN KEY (conversation_id) REFERENCES public.conversations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT messages_content_check CHECK (content ~ '[^[:space:]]'),
    CONSTRAINT messages_conversation_fk
        FOREIGN KEY (conversation_id) REFERENCES public.conversations(id) ON DELETE CASCADE,
    CONSTRAINT messages_sender_participant_fk
        FOREIGN KEY (conversation_id, sender_id)
        REFERENCES public.conversation_participants(conversation_id, user_id)
        ON DELETE NO ACTION
);

-- Chats de un usuario; la PK ya cubre participantes de una conversación.
CREATE INDEX IF NOT EXISTS idx_conversation_participants_user
    ON public.conversation_participants (user_id, conversation_id);
-- Historial con orden estable incluso si coinciden las fechas.
CREATE INDEX IF NOT EXISTS idx_messages_conversation_created
    ON public.messages (conversation_id, created_at DESC, id DESC);
-- Comprobación de la FK al cambiar/eliminar participantes.
CREATE INDEX IF NOT EXISTS idx_messages_conversation_sender
    ON public.messages (conversation_id, sender_id);

COMMIT;
