package com.musa.social.service.impl;

import com.musa.social.dto.request.CreateCommentRequest;
import com.musa.social.dto.request.UpdateCommentRequest;
import com.musa.social.dto.response.CommentResponse;
import com.musa.social.entity.Comment;
import com.musa.social.exception.BadRequestException;
import com.musa.social.exception.ResourceNotFoundException;
import com.musa.social.mapper.CommentMapper;
import com.musa.social.repository.CommentRepository;
import com.musa.social.repository.PostRepository;
import com.musa.social.service.CommentModerationService;
import com.musa.social.service.CommentService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl
        implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentModerationService commentModerationService;


    // Agregar comentario
    @Override
    @Transactional
    public CommentResponse createComment(
            UUID authenticatedUserId,
            UUID postId,
            CreateCommentRequest request
    ) {

        if (postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .isEmpty()) {

            throw new ResourceNotFoundException(
                    "La publicación no existe"
            );
        }

        String normalizedText =
                normalizeText(request.text());

        commentModerationService
                .validateComment(normalizedText);

        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(authenticatedUserId)
                .text(normalizedText)
                .build();

        Comment savedComment =
                commentRepository.save(comment);

        return CommentMapper.toResponse(savedComment);
    }


    // Editar comentario
    @Override
    @Transactional
    public CommentResponse updateComment(
            UUID authenticatedUserId,
            UUID commentId,
            UpdateCommentRequest request
    ) {

        Comment comment = commentRepository
                .findById(commentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "El comentario no existe"
                        )
                );

        if (!comment.getAuthorId().equals(authenticatedUserId)) {
            throw new BadRequestException(
                    "No puedes editar un comentario que no te pertenece"
            );
        }

        String normalizedText =
                normalizeText(request.text());

        commentModerationService
                .validateComment(normalizedText);

        comment.setText(normalizedText);

        Comment updatedComment =
                commentRepository.save(comment);

        return CommentMapper.toResponse(updatedComment);
    }


    // Eliminar comentario
    // Hard delete
    @Override
    @Transactional
    public void deleteComment(
            UUID authenticatedUserId,
            UUID commentId
    ) {

        Comment comment = commentRepository
                .findById(commentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "El comentario no existe"
                        )
                );

        if (!comment.getAuthorId().equals(authenticatedUserId)) {
            throw new BadRequestException(
                    "No puedes eliminar un comentario que no te pertenece"
            );
        }

        commentRepository.delete(comment);
    }


    // Consultar comentarios de una publicación
    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(
            UUID postId
    ) {

        if (postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .isEmpty()) {

            throw new ResourceNotFoundException(
                    "La publicación no existe"
            );
        }

        return commentRepository
                .findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(CommentMapper::toResponse)
                .toList();
    }


    // Utilidades
    private String normalizeText(String text) {

        if (text == null) {
            return null;
        }

        String normalized = text.trim();

        if (normalized.isEmpty()) {
            throw new BadRequestException(
                    "El comentario no puede estar vacío"
            );
        }

        return normalized;
    }
}