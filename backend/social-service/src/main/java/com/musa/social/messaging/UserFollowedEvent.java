package com.musa.social.messaging;
import java.util.UUID;
public record UserFollowedEvent(UUID followerId, UUID followedId) {}
