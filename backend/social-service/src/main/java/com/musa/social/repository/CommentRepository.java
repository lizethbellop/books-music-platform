package com.musa.social.repository;

import com.musa.social.entity.Comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository
        extends JpaRepository<Comment, UUID> {

    List<Comment> findByPostIdOrderByCreatedAtAsc(
            UUID postId
    );

    List<Comment> findByAuthorIdOrderByCreatedAtDesc(
            UUID authorId
    );

    long countByPostId(
            UUID postId
    );
}