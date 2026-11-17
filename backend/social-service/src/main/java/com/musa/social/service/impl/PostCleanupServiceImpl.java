package com.musa.social.service.impl;

import com.musa.social.entity.Post;
import com.musa.social.repository.PostRepository;
import com.musa.social.service.PostCleanupService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostCleanupServiceImpl implements PostCleanupService {

    private final PostRepository postRepository;

    @Value("${musa.social.posts.retention-days:30}")
    private long retentionDays;

    @Override
    @Transactional
    public int deleteExpiredPosts() {

        OffsetDateTime expirationLimit =
                OffsetDateTime.now()
                        .minusDays(retentionDays);

        List<Post> expiredPosts =
                postRepository
                        .findByDeletedAtIsNotNullAndDeletedAtBefore(
                                expirationLimit
                        );

        if (expiredPosts.isEmpty()) {
            return 0;
        }

        postRepository.deleteAll(expiredPosts);

        return expiredPosts.size();
    }
}