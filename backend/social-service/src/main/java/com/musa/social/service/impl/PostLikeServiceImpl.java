package com.musa.social.service.impl;

import com.musa.social.dto.response.PostLikeResponse;
import com.musa.social.entity.PostLike;
import com.musa.social.exception.ConflictException;
import com.musa.social.exception.ResourceNotFoundException;
import com.musa.social.mapper.PostLikeMapper;
import com.musa.social.repository.PostLikeRepository;
import com.musa.social.repository.PostRepository;
import com.musa.social.service.PostLikeService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl
        implements PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;


    // Dar like
    @Override
    @Transactional
    public PostLikeResponse likePost(
            UUID authenticatedUserId,
            UUID postId
    ) {

        // El post debe existir y no estar eliminado
        if (!postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .isPresent()) {

            throw new ResourceNotFoundException(
                    "La publicación no existe"
            );
        }

        // Un usuario solo puede dar un like por publicación
        if (postLikeRepository
                .existsByPostIdAndUserId(
                        postId,
                        authenticatedUserId
                )) {

            throw new ConflictException(
                    "Ya has dado like a esta publicación"
            );
        }

        PostLike postLike = PostLike.builder()
                .postId(postId)
                .userId(authenticatedUserId)
                .build();

        PostLike savedLike =
                postLikeRepository.save(postLike);

        return PostLikeMapper.toResponse(savedLike);
    }


    // Quitar like
    @Override
    @Transactional
    public void unlikePost(
            UUID authenticatedUserId,
            UUID postId
    ) {

        // También verificamos que el post siga activo
        if (!postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .isPresent()) {

            throw new ResourceNotFoundException(
                    "La publicación no existe"
            );
        }

        PostLike postLike = postLikeRepository
                .findByPostIdAndUserId(
                        postId,
                        authenticatedUserId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No has dado like a esta publicación"
                        )
                );

        postLikeRepository.delete(postLike);
    }


    // Consultar cantidad de likes
    @Override
    @Transactional(readOnly = true)
    public long countLikes(UUID postId) {

        if (!postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .isPresent()) {

            throw new ResourceNotFoundException(
                    "La publicación no existe"
            );
        }

        return postLikeRepository.countByPostId(postId);
    }


    // Consultar likes de publicación
    @Override
    @Transactional(readOnly = true)
    public List<PostLikeResponse> getPostLikes(
            UUID postId
    ) {

        if (!postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .isPresent()) {

            throw new ResourceNotFoundException(
                    "La publicación no existe"
            );
        }

        return postLikeRepository
                .findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(PostLikeMapper::toResponse)
                .toList();
    }
}