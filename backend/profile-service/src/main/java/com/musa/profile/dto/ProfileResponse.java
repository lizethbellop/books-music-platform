package com.musa.profile.dto;

import com.musa.profile.entity.Profile;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfileResponse(
     UUID id,
     UUID userId,
     String biography,
     String profilePictureUrl,
     boolean privateProfile,
     OffsetDateTime createdAt,
     OffsetDateTime updatedAt
) {
    public static ProfileResponse from(Profile profile){
        return new ProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getBiography(),
                profile.getProfilePictureUrl(),
                profile.isPrivateProfile(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
