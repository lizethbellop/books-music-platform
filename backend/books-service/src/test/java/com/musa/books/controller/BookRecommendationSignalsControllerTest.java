package com.musa.books.controller;

import com.musa.books.config.CorsConfig;
import com.musa.books.config.JwtConfig;
import com.musa.books.config.SecurityConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
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

@WebMvcTest(BookRecommendationSignalsController.class)
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class})
class BookRecommendationSignalsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean EntityManager entityManager;
    private static final byte[] KEY = new byte[64];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("musa.security.jwt.secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    private String bearer(UUID id) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject(id.toString())
                .issuer("musa-auth").audience("musa-api")
                .expirationTime(Date.from(Instant.now().plusSeconds(60))).build();
        var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        token.sign(new MACSigner(KEY));
        return "Bearer " + token.serialize();
    }

    @Test void requiresAuthenticationBeforeQuerying() throws Exception {
        mvc.perform(get("/books/recommendation-signals/me"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(entityManager);
    }

    @SuppressWarnings("unchecked")
    @Test void queriesTokenOwnerAndNeverTheSuppliedUserId() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        TypedQuery<Object[]> ratings = mock(TypedQuery.class);
        TypedQuery<String> library = mock(TypedQuery.class);
        TypedQuery<String> favorites = mock(TypedQuery.class);
        when(entityManager.createQuery(contains("FROM Review"), eq(Object[].class))).thenReturn(ratings);
        when(entityManager.createQuery(contains("FROM UserBook"), eq(String.class))).thenReturn(library);
        when(ratings.setParameter("userId", owner)).thenReturn(ratings);
        when(library.setParameter("userId", owner)).thenReturn(library);
        when(entityManager.createQuery(contains("FROM FavoriteBook"), eq(String.class))).thenReturn(favorites);
        when(favorites.setParameter("userId", owner)).thenReturn(favorites);
        when(favorites.getResultList()).thenReturn(List.of("OL3W"));
        when(ratings.getResultList()).thenReturn(List.<Object[]>of(new Object[]{"OL1W", new BigDecimal("5.0")}));
        when(library.getResultList()).thenReturn(List.of("OL2W"));

        mvc.perform(get("/books/recommendation-signals/me")
                .header("Authorization", bearer(owner)).param("userId", other.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratings[0].externalId").value("OL1W"))
                .andExpect(jsonPath("$.ratings[0].rating").value(5.0))
                .andExpect(jsonPath("$.libraryIds[0]").value("OL2W"))
                .andExpect(jsonPath("$.favoriteIds[0]").value("OL3W"))
                .andExpect(header().string("Cache-Control", "no-store"));
        verify(ratings).setParameter("userId", owner);
        verify(library).setParameter("userId", owner);
        verify(favorites).setParameter("userId", owner);
        verify(favorites, never()).setParameter("userId", other);
        verify(ratings, never()).setParameter("userId", other);
        verify(library, never()).setParameter("userId", other);
    }
}
