package com.musa.social.dto.request;

import com.musa.social.entity.ContentType;

import jakarta.validation.constraints.Size;

public record UpdatePostRequest(

        @Size(max = 5000, message = "El texto de la publicación es demasiado largo")
        String text,

        ContentType contentType,

        String referenceId
) {
}