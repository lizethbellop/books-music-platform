package com.musa.social.service.impl;

import com.musa.social.entity.ModerationTerm;
import com.musa.social.entity.ModerationTermVariant;
import com.musa.social.exception.UnprocessableEntityException;
import com.musa.social.repository.ModerationTermRepository;
import com.musa.social.repository.ModerationTermVariantRepository;
import com.musa.social.service.CommentModerationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommentModerationServiceImpl
        implements CommentModerationService {

    private final ModerationTermRepository moderationTermRepository;
    private final ModerationTermVariantRepository moderationTermVariantRepository;

    private static final Pattern DIACRITICS =
            Pattern.compile("\\p{M}+");

    private static final Pattern NON_ALPHANUMERIC =
            Pattern.compile("[^a-z0-9]+");

    private static final Pattern REPEATED_CHARACTERS =
            Pattern.compile("(.)\\1{2,}");

    @Override
    public void validateComment(String text) {

        String normalizedComment =
                normalize(text);

        List<ModerationTerm> activeTerms =
                moderationTermRepository
                        .findByActiveTrue();

        if (activeTerms.isEmpty()) {
            return;
        }

        List<UUID> activeTermIds =
                activeTerms.stream()
                        .map(ModerationTerm::getId)
                        .toList();

        List<ModerationTermVariant> variants =
                moderationTermVariantRepository
                        .findByTermIdIn(activeTermIds);

        List<String> forbiddenExpressions =
                new ArrayList<>();

        activeTerms.stream()
                .map(ModerationTerm::getNormalizedTerm)
                .map(this::normalize)
                .forEach(forbiddenExpressions::add);

        variants.stream()
                .map(ModerationTermVariant::getNormalizedVariant)
                .map(this::normalize)
                .forEach(forbiddenExpressions::add);

        List<String> tokens =
                List.of(normalizedComment.split("\\s+"));

        boolean inappropriate =
                tokens.stream()
                        .anyMatch(token ->
                                forbiddenExpressions
                                        .contains(token)
                        );

        if (inappropriate) {
            throw new UnprocessableEntityException(
                    "El comentario contiene contenido no permitido"
            );
        }
    }


    private String normalize(String text) {

        if (text == null) {
            return "";
        }

        String normalized =
                text.toLowerCase(Locale.ROOT);

        normalized =
                Normalizer.normalize(
                        normalized,
                        Normalizer.Form.NFD
                );

        normalized =
                DIACRITICS
                        .matcher(normalized)
                        .replaceAll("");

        normalized =
                normalizeLeetspeak(normalized);

        normalized =
                REPEATED_CHARACTERS
                        .matcher(normalized)
                        .replaceAll("$1");

        normalized =
                NON_ALPHANUMERIC
                        .matcher(normalized)
                        .replaceAll(" ");

        return normalized
                .trim()
                .replaceAll("\\s+", " ");
    }


    private String normalizeLeetspeak(String text) {

        return text
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('3', 'e')
                .replace('4', 'a')
                .replace('5', 's')
                .replace('7', 't');
    }
}