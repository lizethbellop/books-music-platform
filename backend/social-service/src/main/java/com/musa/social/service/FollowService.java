package com.musa.social.service;

import com.musa.social.entity.Follow;

import java.util.List;
import java.util.UUID;

public interface FollowService {

    Follow followUser(UUID followerId, UUID followedId);

    void unfollowUser(UUID followerId, UUID followedId);

    List<Follow> getFollowers(UUID userId);

    List<Follow> getFollowing(UUID userId);
}