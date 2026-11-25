package com.musa.social.service;

import com.musa.social.dto.response.FeedPageResponse;

import java.util.UUID;

public interface FeedService {

    FeedPageResponse getFeed(
            UUID authenticatedUserId,
            int page,
            int size
    );
}