package com.musa.social.service.impl;

import com.musa.social.dto.request.CreatePostRequest;
import com.musa.social.dto.request.UpdatePostRequest;
import com.musa.social.dto.response.PostResponse;
import com.musa.social.entity.Post;
import com.musa.social.entity.PostOrigin;
import com.musa.social.exception.BadRequestException;
import com.musa.social.exception.ResourceNotFoundException;
import com.musa.social.mapper.PostMapper;
import com.musa.social.repository.PostRepository;
import com.musa.social.service.PostService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;


    // CU-47 - Crear publicación
    @Override
    @Transactional
    public PostResponse createPost(
            UUID authenticatedUserId,
            CreatePostRequest request
    ) {

        validateCreateRequest(request);

        PostOrigin origin = request.postOrigin() != null
                ? request.postOrigin()
                : PostOrigin.USER_CREATED;

        Post post = Post.builder()
                .authorId(authenticatedUserId)
                .text(normalizeText(request.text()))
                .postOrigin(origin)
                .activityType(request.activityType())
                .contentType(request.contentType())
                .referenceId(normalizeReferenceId(request.referenceId()))
                .activityData(request.activityData())
                .communityId(request.communityId())
                .build();

        Post savedPost = postRepository.save(post);

        return PostMapper.toResponse(savedPost);
    }


    // CU-46 - Editar publicación
    @Override
    @Transactional
    public PostResponse updatePost(
            UUID authenticatedUserId,
            UUID postId,
            UpdatePostRequest request
    ) {

        Post post = postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La publicación no existe"
                        )
                );

        if (!post.getAuthorId().equals(authenticatedUserId)) {
            throw new BadRequestException(
                    "No puedes editar una publicación que no te pertenece"
            );
        }

        boolean hasReference =
                request.referenceId() != null
                && !request.referenceId().trim().isEmpty();

        boolean hasContentType =
                request.contentType() != null;

        if (hasContentType != hasReference) {
            throw new BadRequestException(
                    "contentType y referenceId deben proporcionarse juntos"
            );
        }

        post.setText(
                normalizeText(request.text())
        );

        post.setContentType(
                request.contentType()
        );

        post.setReferenceId(
                normalizeReferenceId(request.referenceId())
        );

        boolean postWouldBeEmpty =
                post.getText() == null
                && post.getReferenceId() == null
                && post.getActivityType() == null;

        if (postWouldBeEmpty) {
            throw new BadRequestException(
                    "La publicación no puede quedar vacía"
            );
        }

        Post updatedPost = postRepository.save(post);

        return PostMapper.toResponse(updatedPost);
    }


    // CU-56 - Eliminar publicación temporalmente
    // Soft delete
    @Override
    @Transactional
    public void deletePost(
            UUID authenticatedUserId,
            UUID postId
    ) {

        Post post = postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La publicación no existe"
                        )
                );

        if (!post.getAuthorId().equals(authenticatedUserId)) {
            throw new BadRequestException(
                    "No puedes eliminar una publicación que no te pertenece"
            );
        }

        post.setDeletedAt(
                OffsetDateTime.now()
        );

        postRepository.save(post);
    }


    // Restaurar publicación eliminada
    @Override
    @Transactional
    public PostResponse restorePost(
            UUID authenticatedUserId,
            UUID postId
    ) {

        Post post = postRepository
                .findByIdAndAuthorId(
                        postId,
                        authenticatedUserId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La publicación no existe"
                        )
                );

        if (post.getDeletedAt() == null) {
            throw new BadRequestException(
                    "La publicación no está eliminada"
            );
        }

        post.setDeletedAt(null);

        Post restoredPost = postRepository.save(post);

        return PostMapper.toResponse(restoredPost);
    }


    // Eliminar publicación permanentemente
    // Hard delete
    @Override
    @Transactional
    public void permanentlyDeletePost(
            UUID authenticatedUserId,
            UUID postId
    ) {

        Post post = postRepository
                .findByIdAndAuthorId(
                        postId,
                        authenticatedUserId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La publicación no existe"
                        )
                );

        if (post.getDeletedAt() == null) {
            throw new BadRequestException(
                    "Primero debes eliminar temporalmente la publicación"
            );
        }

        postRepository.delete(post);
    }

    // Consultar publicaciones eliminadas recientemente
    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getDeletedPosts(
            UUID authenticatedUserId
    ) {

        return postRepository
                .findByAuthorIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(
                        authenticatedUserId
                )
                .stream()
                .map(PostMapper::toResponse)
                .toList();
    }


    // CU-55 - Consultar publicación por ID
    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(UUID postId) {

        Post post = postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "La publicación no existe"
                        )
                );

        return PostMapper.toResponse(post);
    }


    // CU-55 / CU-31 - Consultar publicaciones por usuario
    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByUser(UUID userId) {

        return postRepository
                .findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(PostMapper::toResponse)
                .toList();
    }


    // CU-55 - Consultar publicaciones por comunidad
    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByCommunity(UUID communityId) {

        return postRepository
                .findByCommunityIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        communityId
                )
                .stream()
                .map(PostMapper::toResponse)
                .toList();
    }


    // Validaciones para creación de publicaciones
    private void validateCreateRequest(CreatePostRequest request) {

        PostOrigin origin = request.postOrigin() != null
                ? request.postOrigin()
                : PostOrigin.USER_CREATED;

        boolean hasText =
                request.text() != null
                && !request.text().trim().isEmpty();

        boolean hasReference =
                request.referenceId() != null
                && !request.referenceId().trim().isEmpty();

        boolean hasContentType =
                request.contentType() != null;

        boolean hasActivity =
                request.activityType() != null;


        // No permitir una publicación completamente vacía
        if (!hasText && !hasReference && !hasActivity) {
            throw new BadRequestException(
                    "La publicación debe contener texto, contenido asociado o una actividad"
            );
        }


        // contentType y referenceId deben venir juntos
        if (hasContentType != hasReference) {
            throw new BadRequestException(
                    "contentType y referenceId deben proporcionarse juntos"
            );
        }


        // USER_CREATED no puede tener activityType
        if (origin == PostOrigin.USER_CREATED
                && request.activityType() != null) {

            throw new BadRequestException(
                    "Una publicación creada por el usuario no puede incluir activityType"
            );
        }


        // ACTIVITY requiere activityType
        if (origin == PostOrigin.ACTIVITY
                && request.activityType() == null) {

            throw new BadRequestException(
                    "Una publicación de actividad requiere activityType"
            );
        }


        // activityData solo puede utilizarse en ACTIVITY
        if (request.activityData() != null
                && origin != PostOrigin.ACTIVITY) {

            throw new BadRequestException(
                    "activityData solo puede utilizarse en publicaciones de actividad"
            );
        }
    }


    // Utilidades
    private String normalizeText(String text) {

        if (text == null) {
            return null;
        }

        String normalized = text.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }


    private String normalizeReferenceId(String referenceId) {

        if (referenceId == null) {
            return null;
        }

        String normalized = referenceId.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}