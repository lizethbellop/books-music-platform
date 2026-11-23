package com.musa.notifications.dto;
import java.util.List;
public record NotificationPage(List<NotificationResponse> items, boolean hasMore, Integer nextPage) {
    public NotificationPage { items = List.copyOf(items); }
}
