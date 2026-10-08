package com.musa.books.config;

import com.musa.books.controller.*;
import com.musa.books.service.*;
import java.util.List;
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

@WebMvcTest({UserBookController.class, FavoriteBookController.class, ReviewController.class, BookController.class})
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class})
class BooksSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserBookService library;
    @MockitoBean FavoriteBookService favorites;
    @MockitoBean ReviewService reviews;
    @MockitoBean OpenLibraryService openLibrary;
    @MockitoBean BookService books;
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

    @Test void rejectsMissingToken() throws Exception {
        mvc.perform(get("/books/library")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(library);
    }

    @Test void rejectsMalformedToken() throws Exception {
        mvc.perform(get("/books/library").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(library);
    }

    @Test void readsLibraryUsingTokenUuidWithoutQueryParameter() throws Exception {
        UUID userId = UUID.randomUUID();
        when(library.getBooksByUser(userId)).thenReturn(List.of());
        mvc.perform(get("/books/library").header("Authorization", bearer(userId)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(library).getBooksByUser(userId);
    }

    @Test void acceptsMatchingLegacyUserParameter() throws Exception {
        UUID userId = UUID.randomUUID();
        when(favorites.getFavoritesByUser(userId)).thenReturn(List.of());
        mvc.perform(get("/books/favorites").header("Authorization", bearer(userId))
                        .param("userId", userId.toString()))
                .andExpect(status().isOk());
        verify(favorites).getFavoritesByUser(userId);
    }

    @Test void rejectsForgedQueryForPrivateReadsAndWrites() throws Exception {
        String token = bearer(UUID.randomUUID());
        String other = UUID.randomUUID().toString();
        mvc.perform(get("/books/library").header("Authorization", token).param("userId", other))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/books/OL1W/favorites").header("Authorization", token).param("userId", other))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/books/OL1W/reviews").header("Authorization", token).param("userId", other))
                .andExpect(status().isForbidden());
        mvc.perform(put("/books/OL1W/rating").header("Authorization", token).param("userId", other)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":3.5}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(library, favorites, reviews);
    }

    @Test void deletesOnlyOwnFavoriteAndReview() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = bearer(userId);
        mvc.perform(delete("/books/OL1W/favorites").header("Authorization", token))
                .andExpect(status().isNoContent());
        verify(favorites).removeFavorite(userId, "OL1W");
        mvc.perform(delete("/books/OL1W/reviews").header("Authorization", token))
                .andExpect(status().isNoContent());
        verify(reviews).deleteReview(userId, "OL1W");
    }

    @Test void preservesAuthenticatedSearch() throws Exception {
        when(openLibrary.searchBooks("test")).thenReturn(List.of());
        mvc.perform(get("/books/search").param("q", "test")
                        .header("Authorization", bearer(UUID.randomUUID())))
                .andExpect(status().isOk());
        verify(openLibrary).searchBooks("test");
    }

    @Test void permitsBrowserPreflightWithAuthorization() throws Exception {
        mvc.perform(options("/books/OL1W/rating").header("Origin", "http://localhost:64514")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:64514"));
    }
}
