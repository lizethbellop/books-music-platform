package com.musa.profile.dto;

import com.musa.profile.entity.ListElement;
import com.musa.profile.entity.UserList;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserListDetailResponse(
        UUID id,
        String name,
        String description,
        OffsetDateTime createdAt,
        List<ListElementResponse> elements
) {
    public static UserListDetailResponse from(
            UserList list,
            List<ListElement> elements
    ) {
        return new UserListDetailResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getCreatedAt(),
                elements.stream()
                        .map(ListElementResponse::from)
                        .toList()
        );
    }
}