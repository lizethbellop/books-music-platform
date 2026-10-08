package com.musa.music;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
@Tag("integration")
class MusicUuidHttpIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Value("${musa.security.jwt.secret}") String signingKey;

    private String bearer(UUID userId) throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .subject(userId.toString()).issuer("musa-auth")
                .audience("musa-api").issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(900)))
                .build();
        var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        token.sign(new MACSigner(Base64.getDecoder().decode(signingKey)));
        return "Bearer " + token.serialize();
    }

    @Test void persistsActionsAndProtectsReviewOwnershipThroughHttp() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        String spotifyId = "uuid-http-" + UUID.randomUUID();
        String firstToken = bearer(first);
        String secondToken = bearer(second);

        for (UUID userId : new UUID[]{first, second}) {
            String token = bearer(userId);
            String fields = "\"userId\":\"%s\",\"spotifyId\":\"%s\",\"contentType\":\"SONG\",\"name\":\"UUID HTTP test\""
                    .formatted(userId, spotifyId);
            mvc.perform(post("/api/music/ratings").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{" + fields + ",\"rating\":3.5}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(userId.toString()));
            mvc.perform(post("/api/music/favorites").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{" + fields + "}"))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/music/reviews").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{" + fields + ",\"reviewText\":\"Original\"}"))
                    .andExpect(status().isOk());
        }

        Long reviewId = jdbc.queryForObject(
                "SELECT r.id FROM music_review r JOIN music_content c ON c.id = r.music_content_id WHERE r.user_id = ? AND c.spotify_id = ?",
                Long.class, first, spotifyId);

        mvc.perform(put("/api/music/reviews/" + reviewId).header("Authorization", secondToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reviewText\":\"Ajena\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/music/reviews/" + reviewId).header("Authorization", secondToken))
                .andExpect(status().isForbidden());
        assertEquals("Original", jdbc.queryForObject("SELECT review_text FROM music_review WHERE id = ?", String.class, reviewId));

        mvc.perform(put("/api/music/reviews/" + reviewId).header("Authorization", firstToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reviewText\":\"Actualizada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewText").value("Actualizada"));
        mvc.perform(get("/api/music/reviews/user/" + first + "/reviewed").header("Authorization", firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].spotifyId").value(spotifyId));

        mvc.perform(delete("/api/music/favorites").header("Authorization", firstToken)
                        .param("spotifyId", spotifyId).param("contentType", "SONG"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/music/favorites/user/" + first).header("Authorization", firstToken)
                        .param("spotifyId", spotifyId).param("contentType", "SONG"))
                .andExpect(status().isOk()).andExpect(content().string("false"));
        mvc.perform(get("/api/music/favorites/user/" + second).header("Authorization", secondToken)
                        .param("spotifyId", spotifyId).param("contentType", "SONG"))
                .andExpect(status().isOk()).andExpect(content().string("true"));
        mvc.perform(get("/api/music/ratings/user/" + first).header("Authorization", firstToken)
                        .param("spotifyId", spotifyId).param("contentType", "SONG"))
                .andExpect(status().isOk()).andExpect(content().string("3.5"));
        mvc.perform(delete("/api/music/reviews/" + reviewId).header("Authorization", firstToken))
                .andExpect(status().isOk());
        // Flush deferred JPA deletes before checking the real PostgreSQL rows.
        entityManager.flush();
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM music_review WHERE id = ?", Long.class, reviewId));
    }

    @Autowired jakarta.persistence.EntityManager entityManager;
}
