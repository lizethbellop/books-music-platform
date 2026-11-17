package com.musa.social.repository;

import com.musa.social.entity.Post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository
        extends JpaRepository<Post, UUID> {

    Optional<Post> findByIdAndDeletedAtIsNull(
            UUID id
    );

    List<Post> findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID authorId
    );

    List<Post> findByCommunityIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID communityId
    );

    List<Post> findByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<Post> findByIdAndAuthorId(
            UUID id,
            UUID authorId
    );

    List<Post> findByAuthorIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(
            UUID authorId
    );

    List<Post> findByDeletedAtIsNotNullAndDeletedAtBefore(
            OffsetDateTime limit
    );
}