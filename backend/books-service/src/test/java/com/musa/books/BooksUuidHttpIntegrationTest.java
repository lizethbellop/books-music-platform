package com.musa.books;

import com.musa.books.entity.Book;
import jakarta.persistence.EntityManager;
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
class BooksUuidHttpIntegrationTest {
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

    @Autowired EntityManager entityManager;

    @Test void persistsIndependentLibrariesFavoritesRatingsAndReviews() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        String externalId = "uuid-http-" + UUID.randomUUID();
        Book book = Book.builder().externalId(externalId).title("UUID HTTP test").build();
        entityManager.persist(book);
        entityManager.flush();
        String firstToken = bearer(first);
        String secondToken = bearer(second);

        for (UUID userId : new UUID[]{first, second}) {
            String token = bearer(userId);
            String status = userId.equals(first) ? "LEYENDO" : "POR_LEER";
            mvc.perform(put("/books/" + externalId + "/reading-status").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.readingStatus").value(status));
            mvc.perform(post("/books/" + externalId + "/favorites").header("Authorization", token))
                    .andExpect(status().isOk());
            mvc.perform(put("/books/" + externalId + "/rating").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":3.5}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(userId.toString()));
            mvc.perform(post("/books/" + externalId + "/reviews").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":3.5,\"reviewText\":\"Original\"}"))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/books/library").header("Authorization", firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].readingStatus").value("LEYENDO"));
        mvc.perform(get("/books/library").header("Authorization", secondToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].readingStatus").value("POR_LEER"));
        mvc.perform(put("/books/" + externalId + "/reviews").header("Authorization", secondToken)
                        .param("userId", first.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4.0,\"reviewText\":\"Ajena\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/books/" + externalId + "/reviews").header("Authorization", firstToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4.0,\"reviewText\":\"Actualizada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewText").value("Actualizada"));
        mvc.perform(delete("/books/" + externalId + "/favorites").header("Authorization", firstToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/books/favorites").header("Authorization", firstToken))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/books/favorites").header("Authorization", secondToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].externalId").value(externalId));
        mvc.perform(delete("/books/" + externalId + "/reviews").header("Authorization", firstToken))
                .andExpect(status().isNoContent());
        entityManager.flush();
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM review WHERE book_id = ?", Long.class, book.getId()));
    }
}
