ALTER TABLE community_activities DROP CONSTRAINT community_activities_status_check;

ALTER TABLE community_activities
ADD CONSTRAINT community_activities_status_check
CHECK (status IN ('PROGRAMADA', 'CANCELADA', 'COMPLETADA', 'ELIMINADA'));


ALTER TABLE community_members
DROP CONSTRAINT IF EXISTS community_members_status_check;

ALTER TABLE community_members
ADD CONSTRAINT community_members_status_check
CHECK (status IN ('ACTIVO', 'PENDIENTE', 'RETIRADO'));