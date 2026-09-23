CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    biography VARCHAR(500),
    profile_picture_url VARCHAR(1000),
    profile_picture_public_id VARCHAR(255),
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_lists_profile
        FOREIGN KEY (profile_id)
        REFERENCES profiles(id)
        ON DELETE CASCADE
);

CREATE TABLE list_elements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    list_id UUID NOT NULL,
    element_type VARCHAR(20) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_list_elements_list
        FOREIGN KEY (list_id)
        REFERENCES user_lists(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_list_element_type
        CHECK (element_type IN ('BOOK', 'SONG', 'ARTIST')),

    CONSTRAINT uk_list_element
        UNIQUE (list_id, element_type, reference_id)
);

CREATE TABLE preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_preferences_profile
        FOREIGN KEY (profile_id)
        REFERENCES profiles(id)
        ON DELETE CASCADE
);

CREATE TABLE preference_elements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    preference_id UUID NOT NULL,
    element_type VARCHAR(20) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_preference_elements_preference
        FOREIGN KEY (preference_id)
        REFERENCES preferences(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_preference_element_type
        CHECK (element_type IN ('BOOK', 'SONG', 'ARTIST', 'AUTHOR')),

    CONSTRAINT uk_preference_element
        UNIQUE (preference_id, element_type, reference_id)
);