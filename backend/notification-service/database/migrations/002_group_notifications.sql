BEGIN;
DO $$ BEGIN
    IF current_database() <> 'notifications_db' THEN RAISE EXCEPTION 'Conéctate a notifications_db'; END IF;
END $$;
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check CHECK (type IN (
    'CHAT_MESSAGE_CREATED', 'USER_FOLLOWED', 'POST_LIKED', 'POST_COMMENTED', 'POST_PUBLISHED',
    'GROUP_MEMBER_ADDED', 'GROUP_MEMBER_REMOVED', 'GROUP_ADMIN_CHANGED'
));
COMMIT;
