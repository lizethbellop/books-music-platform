package com.musa.social.service.impl;

import com.musa.social.dto.response.FeedPageResponse;
import com.musa.social.dto.response.FeedPostResponse;
import com.musa.social.entity.Follow;
import com.musa.social.entity.Post;
import com.musa.social.exception.BadRequestException;
import com.musa.social.repository.CommentRepository;
import com.musa.social.repository.FollowRepository;
import com.musa.social.repository.PostLikeRepository;
import com.musa.social.repository.PostRepository;
import com.musa.social.service.FeedService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;


    // =========================================================
    // Feed social paginado
    //
    // - publicaciones propias
    // - publicaciones de usuarios seguidos
    // - excluye publicaciones de comunidades
    // - excluye publicaciones eliminadas
    // - orden cronológico descendente
    // - incluye conteo de likes y comentarios
    // - indica si el usuario autenticado ya dio like
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public FeedPageResponse getFeed(
            UUID authenticatedUserId,
            int page,
            int size
    ) {

        // -----------------------------------------------------
        // Validar parámetros de paginación
        // -----------------------------------------------------
        if (page < 0) {
            throw new BadRequestException(
                    "La página no puede ser negativa"
            );
        }

        if (size < 1 || size > 100) {
            throw new BadRequestException(
                    "El tamaño de página debe estar entre 1 y 100"
            );
        }


        // -----------------------------------------------------
        // Obtener usuarios seguidos
        // -----------------------------------------------------
        List<Follow> followingRelations =
                followRepository.findByFollowerId(
                        authenticatedUserId
                );


        // -----------------------------------------------------
        // Autores que aparecerán en el feed
        //
        // Incluye:
        // - usuario autenticado
        // - usuarios que sigue
        // -----------------------------------------------------
        List<UUID> feedAuthorIds =
                new ArrayList<>();

        feedAuthorIds.add(authenticatedUserId);

        followingRelations.stream()
                .map(Follow::getFollowedId)
                .forEach(feedAuthorIds::add);


        // -----------------------------------------------------
        // Configurar paginación
        // -----------------------------------------------------
        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );


        // -----------------------------------------------------
        // Consultar publicaciones activas del feed
        // -----------------------------------------------------
        Page<Post> postPage =
                postRepository
                        .findByAuthorIdInAndCommunityIdIsNullAndDeletedAtIsNull(
                                feedAuthorIds,
                                pageable
                        );


        // -----------------------------------------------------
        // Convertir Posts a respuestas del feed
        // -----------------------------------------------------
        List<FeedPostResponse> content =
                postPage.getContent()
                        .stream()
                        .map(post ->
                                toFeedResponse(
                                        post,
                                        authenticatedUserId
                                )
                        )
                        .toList();


        // -----------------------------------------------------
        // Respuesta paginada
        // -----------------------------------------------------
        return new FeedPageResponse(
                content,
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.isFirst(),
                postPage.isLast()
        );
    }


    // =========================================================
    // Construir respuesta individual del feed
    // =========================================================
    private FeedPostResponse toFeedResponse(
            Post post,
            UUID authenticatedUserId
    ) {

        long likeCount =
                postLikeRepository.countByPostId(
                        post.getId()
                );

        long commentCount =
                commentRepository.countByPostId(
                        post.getId()
                );

        boolean likedByMe =
                postLikeRepository
                        .existsByPostIdAndUserId(
                                post.getId(),
                                authenticatedUserId
                        );


        return new FeedPostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getText(),
                post.getPostOrigin(),
                post.getActivityType(),
                post.getContentType(),
                post.getReferenceId(),
                post.getActivityData(),
                post.getCommunityId(),
                likeCount,
                commentCount,
                likedByMe,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}