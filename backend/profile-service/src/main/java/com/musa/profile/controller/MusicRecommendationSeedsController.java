package com.musa.profile.controller;

import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
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
@RequestMapping("/api/profiles/me")
public class MusicRecommendationSeedsController {
    private final EntityManager entityManager;

    public MusicRecommendationSeedsController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public record Ref(String contentType, String externalId) {}
    public record Seeds(List<Ref> items) {}

    @GetMapping("/music-seeds")
    @Transactional(readOnly = true)
    public ResponseEntity<Seeds> getOwnSeeds(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var result = new LinkedHashMap<String, Ref>();
        var listed = entityManager.createQuery("""
                SELECT DISTINCT e.elementType, e.referenceId FROM ListElement e
                WHERE e.elementType IN ('SONG', 'ALBUM', 'ARTIST')
                  AND e.listId IN (SELECT l.id FROM UserList l
                    WHERE l.profileId IN (SELECT p.id FROM Profile p WHERE p.userId = :userId))
                ORDER BY e.elementType, e.referenceId
                """, Object[].class).setParameter("userId", userId).getResultList();
        var preferences = entityManager.createQuery("""
                SELECT DISTINCT e.elementType, e.referenceId FROM PreferenceElement e
                WHERE e.elementType IN ('SONG', 'ALBUM', 'ARTIST')
                  AND e.preferenceId IN (SELECT pref.id FROM Preference pref
                    WHERE pref.profileId IN (SELECT p.id FROM Profile p WHERE p.userId = :userId))
                ORDER BY e.elementType, e.referenceId
                """, Object[].class).setParameter("userId", userId).getResultList();
        for (var rows : List.of(listed, preferences)) {
            for (var row : rows) {
                var ref = new Ref((String) row[0], (String) row[1]);
                result.putIfAbsent(ref.contentType() + ":" + ref.externalId(), ref);
            }
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("Vary", "Authorization").body(new Seeds(List.copyOf(result.values())));
    }
}
