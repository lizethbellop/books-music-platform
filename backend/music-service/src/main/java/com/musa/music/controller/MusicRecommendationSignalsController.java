package com.musa.music.controller;

import com.musa.music.entity.MusicContentType;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/music")
public class MusicRecommendationSignalsController {
    private final EntityManager entityManager;

    public MusicRecommendationSignalsController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public record Ref(MusicContentType contentType, String externalId) {}
    public record Rating(MusicContentType contentType, String externalId, Double rating) {}
    public record Signals(List<Rating> ratings, List<Ref> favorites, List<Ref> reviewed) {}

    @GetMapping("/recommendation-signals/me")
    @Transactional(readOnly = true)
    public ResponseEntity<Signals> getOwnSignals(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<Rating> ratings = entityManager.createQuery("""
                SELECT r.musicContent.contentType, r.musicContent.spotifyId, r.rating
                FROM MusicRating r WHERE r.userId = :userId
                ORDER BY r.musicContent.contentType, r.musicContent.spotifyId
                """, Object[].class)
                .setParameter("userId", userId).getResultList().stream()
                .map(row -> new Rating((MusicContentType) row[0], (String) row[1], (Double) row[2]))
                .toList();
        List<Ref> favorites = entityManager.createQuery("""
                SELECT f.musicContent.contentType, f.musicContent.spotifyId
                FROM MusicFavorite f WHERE f.userId = :userId
                ORDER BY f.musicContent.contentType, f.musicContent.spotifyId
                """, Object[].class)
                .setParameter("userId", userId).getResultList().stream()
                .map(row -> new Ref((MusicContentType) row[0], (String) row[1])).toList();
        List<Ref> reviewed = entityManager.createQuery("""
                SELECT r.musicContent.contentType, r.musicContent.spotifyId
                FROM MusicReview r WHERE r.userId = :userId
                ORDER BY r.musicContent.contentType, r.musicContent.spotifyId
                """, Object[].class)
                .setParameter("userId", userId).getResultList().stream()
                .map(row -> new Ref((MusicContentType) row[0], (String) row[1])).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Vary", "Authorization").body(new Signals(ratings, favorites, reviewed));
    }
}
