package com.musa.profile.dto;

import com.musa.profile.entity.UserList;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserListResponse(
        UUID id,
        String name,
        String description,
        OffsetDateTime createdAt
) {
    public static UserListResponse from(UserList list) {
        return new UserListResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getCreatedAt()
        );
    }
}