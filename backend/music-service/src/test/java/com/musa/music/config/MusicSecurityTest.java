package com.musa.music.config;

import com.musa.music.controller.MusicUserActionsController;
import com.musa.music.controller.MusicReviewController;
import com.musa.music.controller.MusicSearchController;
import com.musa.music.dto.MusicRatingRequest;
import com.musa.music.entity.MusicContentType;
import com.musa.music.entity.MusicRating;
import com.musa.music.service.*;
import org.mockito.ArgumentCaptor;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({MusicUserActionsController.class, MusicReviewController.class, MusicSearchController.class})
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class})
class MusicSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean MusicRatingService ratings;
    @MockitoBean MusicFavoriteService favorites;
    @MockitoBean MusicReviewService reviews;
    @MockitoBean MusicSearchService search;
    private static final byte[] KEY = new byte[64];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("musa.security.jwt.secret",
                () -> Base64.getEncoder().encodeToString(KEY));
    }

    private String bearer(UUID userId) throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .subject(userId.toString()).issuer("musa-auth")
                .audience("musa-api").issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(900)))
                .build();
        var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        token.sign(new MACSigner(KEY));
        return "Bearer " + token.serialize();
    }

    private String ratingBody(UUID userId) {
        return """
                {"userId":"%s","spotifyId":"song-test","contentType":"SONG",
                 "name":"Canción de prueba","rating":3.5}
                """.formatted(userId);
    }

    @Test void rejectsMissingToken() throws Exception {
        mvc.perform(post("/api/music/ratings").contentType(MediaType.APPLICATION_JSON)
                        .content(ratingBody(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(ratings);
    }

    @Test void rejectsInvalidSignatureOrMalformedToken() throws Exception {
        mvc.perform(get("/api/music/search").param("q", "test")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(search);
    }

    @Test void acceptsRealSignedTokenForSearch() throws Exception {
        when(search.search("test")).thenReturn(java.util.List.of());
        mvc.perform(get("/api/music/search").param("q", "test")
                        .header("Authorization", bearer(UUID.randomUUID())))
                .andExpect(status().isOk());
        verify(search).search("test");
    }

    @Test void savesRatingForAuthenticatedUuid() throws Exception {
        UUID userId = UUID.randomUUID();
        when(ratings.saveOrUpdateRating(any())).thenReturn(
                MusicRating.builder().userId(userId).rating(3.5).build());
        mvc.perform(post("/api/music/ratings")
                        .header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(ratingBody(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
        var captor = ArgumentCaptor.forClass(MusicRatingRequest.class);
        verify(ratings).saveOrUpdateRating(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(userId, captor.getValue().userId());
    }

    @Test void rejectsForgedBodyUser() throws Exception {
        mvc.perform(post("/api/music/ratings")
                        .header("Authorization", bearer(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON).content(ratingBody(UUID.randomUUID())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ratings);
    }

    @Test void rejectsOtherUsersPrivateQueries() throws Exception {
        UUID other = UUID.randomUUID();
        String token = bearer(UUID.randomUUID());
        for (String path : new String[]{"/api/music/ratings/user/", "/api/music/favorites/user/", "/api/music/reviews/user/"}) {
            mvc.perform(get(path + other).header("Authorization", token)
                            .param("spotifyId", "song-test").param("contentType", "SONG"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/music/reviews/user/" + other + "/reviewed")
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ratings, favorites, reviews);
    }

    @Test void favoriteDeletionUsesTokenWithoutLegacyQuery() throws Exception {
        UUID userId = UUID.randomUUID();
        mvc.perform(delete("/api/music/favorites").header("Authorization", bearer(userId))
                        .param("spotifyId", "song-test").param("contentType", "SONG"))
                .andExpect(status().isNoContent());
        verify(favorites).removeFavorite(userId, "song-test", MusicContentType.SONG);
    }

    @Test void rejectsForgedFavoriteDeletionQuery() throws Exception {
        mvc.perform(delete("/api/music/favorites")
                        .header("Authorization", bearer(UUID.randomUUID()))
                        .param("userId", UUID.randomUUID().toString())
                        .param("spotifyId", "song-test").param("contentType", "SONG"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(favorites);
    }

    @Test void forwardsAuthenticatedUserForReviewEditAndDelete() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = bearer(userId);
        mvc.perform(put("/api/music/reviews/7").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reviewText\":\"Cambio\"}"))
                .andExpect(status().isOk());
        verify(reviews).updateReview(eq(7L), eq(userId), any());
        mvc.perform(delete("/api/music/reviews/7").header("Authorization", token))
                .andExpect(status().isOk());
        verify(reviews).deleteReview(7L, userId);
    }

    @Test void permitsBrowserPreflightWithAuthorization() throws Exception {
        mvc.perform(options("/api/music/ratings").header("Origin", "http://localhost:64514")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:64514"));
    }
}
