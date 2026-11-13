package com.musa.music.controller;

import com.musa.music.config.*;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.musa.music.entity.MusicContentType;

@WebMvcTest(MusicRecommendationSignalsController.class)
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class})
class MusicRecommendationSignalsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean EntityManager entityManager;
    private static final byte[] KEY = new byte[64];
    static { new SecureRandom().nextBytes(KEY); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("musa.security.jwt.secret", () -> Base64.getEncoder().encodeToString(KEY));
    }
    private String bearer(UUID id) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject(id.toString()).issuer("musa-auth")
                .audience("musa-api").expirationTime(Date.from(Instant.now().plusSeconds(60))).build();
        var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        token.sign(new MACSigner(KEY));
        return "Bearer " + token.serialize();
    }
    @Test void rejectsUnauthenticatedReads() throws Exception {
        mvc.perform(get("/api/music/recommendation-signals/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(entityManager);
    }
    @SuppressWarnings("unchecked")
    @Test void scopesAllQueriesToTheTokenOwner() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
        when(query.setParameter("userId", owner)).thenReturn(query);
        when(query.getResultList()).thenReturn(
                List.<Object[]>of(new Object[]{MusicContentType.ALBUM, "album-one", 5.0}),
                List.<Object[]>of(new Object[]{MusicContentType.ARTIST, "artist-one"}),
                List.<Object[]>of(new Object[]{MusicContentType.SONG, "song-one"}));
        mvc.perform(get("/api/music/recommendation-signals/me").header("Authorization", bearer(owner))
                .param("userId", other.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.ratings[0].contentType").value("ALBUM"))
                .andExpect(jsonPath("$.favorites[0].contentType").value("ARTIST"))
                .andExpect(jsonPath("$.reviewed[0].contentType").value("SONG"));
        verify(query, times(3)).setParameter("userId", owner);
        verify(query, never()).setParameter("userId", other);
    }
}
