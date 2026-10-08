package com.musa.profile.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.musa.profile.entity.UserList;

public record UserListDetailResponse(
        UUID id,
        String name,
        String description,
        OffsetDateTime createdAt,
        List<ResolvedListElementResponse> elements
) {

    public static UserListDetailResponse from(
            UserList list,
            List<ResolvedListElementResponse> elements
    ) {
        return new UserListDetailResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getCreatedAt(),
                elements
        );
    }
}