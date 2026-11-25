package com.musa.social.repository;

import com.musa.social.entity.Post;
import com.musa.social.entity.PostOrigin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface PostRepository
        extends JpaRepository<Post, UUID> {

        Page<Post> findByAuthorIdInAndCommunityIdIsNullAndDeletedAtIsNull(
                List<UUID> authorIds,
                Pageable pageable
        );

    Optional<Post> findByIdAndDeletedAtIsNull(
            UUID id
    );

    List<Post> findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID authorId,
            PostOrigin postOrigin

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
    
    List<Post> findByAuthorIdAndPostOriginAndDeletedAtIsNullOrderByCreatedAtDesc(
        UUID authorId,
        PostOrigin postOrigin
    );

}