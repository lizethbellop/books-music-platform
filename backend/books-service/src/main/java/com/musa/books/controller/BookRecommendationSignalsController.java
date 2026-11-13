package com.musa.books.controller;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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
@RequestMapping("/books")
public class BookRecommendationSignalsController {
    private final EntityManager entityManager;

    public BookRecommendationSignalsController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public record BookRating(String externalId, BigDecimal rating) {}
    public record Signals(List<BookRating> ratings, List<String> libraryIds, List<String> favoriteIds) {}

    @GetMapping("/recommendation-signals/me")
    @Transactional(readOnly = true)
    public ResponseEntity<Signals> getOwnSignals(@AuthenticationPrincipal Jwt jwt) {
        // La identidad viene del JWT. No se recibe userId desde el cliente.
        UUID userId = UUID.fromString(jwt.getSubject());

        // Proyecciones: solo IDs y notas, sin cargar textos de reseñas
        // ni ejecutar una consulta adicional por cada libro.
        List<BookRating> ratings = entityManager.createQuery("""
                SELECT r.book.externalId, r.rating
                FROM Review r WHERE r.userId = :userId
                ORDER BY r.book.externalId
                """, Object[].class)
                .setParameter("userId", userId)
                .getResultList().stream()
                .map(row -> new BookRating((String) row[0], (BigDecimal) row[1]))
                .toList();

        List<String> libraryIds = entityManager.createQuery("""
                SELECT DISTINCT u.book.externalId
                FROM UserBook u WHERE u.userId = :userId
                ORDER BY u.book.externalId
                """, String.class)
                .setParameter("userId", userId)
                .getResultList();

        List<String> favoriteIds = entityManager.createQuery("""
                SELECT DISTINCT f.book.externalId FROM FavoriteBook f
                WHERE f.userId = :userId ORDER BY f.book.externalId
                """, String.class).setParameter("userId", userId).getResultList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Vary", "Authorization")
                .body(new Signals(ratings, libraryIds, favoriteIds));
    }
}
