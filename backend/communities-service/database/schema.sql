BEGIN;

CREATE TABLE communities (
     id UUID PRIMARY KEY,
     name VARCHAR(100) NOT NULL,
     description VARCHAR(1000),
     type VARCHAR(20) NOT NULL CHECK (type IN ('BOOKS', 'MUSIC', 'MIXED')),
     visibility VARCHAR(20) NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
     created_by UUID NOT NULL,
     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
     status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DELETED')),
     CONSTRAINT chk_community_name CHECK (length(trim(name)) > 0)
);

CREATE TABLE community_members (
       id UUID PRIMARY KEY,
       community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
       user_id UUID NOT NULL,
       role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'MEMBER')),
       joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LEFT')),
       CONSTRAINT uq_community_member UNIQUE (community_id, user_id)
);

CREATE TABLE community_activities (
      id UUID PRIMARY KEY,
      community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
      created_by UUID NOT NULL,
      title VARCHAR(150) NOT NULL,
      description VARCHAR(1000),
      location VARCHAR(255),
      starts_at TIMESTAMPTZ NOT NULL,
      ends_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED')),
      CONSTRAINT chk_activity_dates CHECK (ends_at IS NULL OR ends_at > starts_at),
      CONSTRAINT chk_activity_title CHECK (length(trim(title)) > 0)
);

CREATE TABLE activity_participants (
       id UUID PRIMARY KEY,
       activity_id UUID NOT NULL REFERENCES community_activities(id) ON DELETE CASCADE,
       user_id UUID NOT NULL,
       status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'CANCELLED')),
       confirmed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT uq_activity_participant UNIQUE (activity_id, user_id)
);

CREATE INDEX idx_communities_name ON communities (name);
CREATE INDEX idx_communities_created_by ON communities (created_by);
CREATE INDEX idx_members_user_status ON community_members (user_id, status);
CREATE INDEX idx_members_community_status ON community_members (community_id, status);
CREATE INDEX idx_activities_community_start ON community_activities (community_id, starts_at);
CREATE INDEX idx_participants_user ON activity_participants (user_id);

COMMIT;