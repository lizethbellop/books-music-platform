package com.musa.social.repository;

import com.musa.social.entity.PostLike;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostLikeRepository
        extends JpaRepository<PostLike, UUID> {

    boolean existsByPostIdAndUserId(
            UUID postId,
            UUID userId
    );

    Optional<PostLike> findByPostIdAndUserId(
            UUID postId,
            UUID userId
    );

    long countByPostId(
            UUID postId
    );

    List<PostLike> findByPostIdOrderByCreatedAtDesc(
            UUID postId
    );
}