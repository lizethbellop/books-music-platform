package com.musa.profile.service;

import com.musa.profile.dto.AddListElementRequest;
import com.musa.profile.dto.catalog.BookCatalogResponse;
import com.musa.profile.dto.catalog.CatalogResolution;
import com.musa.profile.entity.Profile;
import com.musa.profile.entity.UserList;
import com.musa.profile.exception.ListElementAlreadyExistsException;
import com.musa.profile.repository.ProfileRepository;
import com.musa.profile.repository.UserListRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.import=optional:file:.env[.properties]",
        "spring.datasource.url=${PROFILE_DB_URL}",
        "spring.datasource.username=${PROFILE_DB_USERNAME}",
        "spring.datasource.password=${PROFILE_DB_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(
        named = "MUSA_PROFILE_DB_TEST",
        matches = "true"
)
class UserListPostgresTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProfileRepository profiles;
    @Autowired UserListRepository lists;
    @Autowired UserListService service;

    @MockitoBean
    CatalogLookupService catalog;

    private UUID userId;
    private UUID otherUserId;
    private UUID profileId;
    private UUID otherProfileId;
    private UUID listId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        profileId = profiles.saveAndFlush(
                new Profile(userId, false)
        ).getId();

        otherProfileId = profiles.saveAndFlush(
                new Profile(otherUserId, false)
        ).getId();

        listId = lists.saveAndFlush(
                new UserList(profileId, "Lista de prueba", null)
        ).getId();
    }

    @AfterEach
    void cleanUp() {
        if (listId != null) {
            jdbc.update(
                    "DELETE FROM list_elements WHERE list_id = ?",
                    listId
            );
            jdbc.update(
                    "DELETE FROM user_lists WHERE id = ?",
                    listId
            );
        }

        if (profileId != null) {
            jdbc.update(
                    "DELETE FROM profiles WHERE id = ?",
                    profileId
            );
        }

        if (otherProfileId != null) {
            jdbc.update(
                    "DELETE FROM profiles WHERE id = ?",
                    otherProfileId
            );
        }
    }

    private CatalogResolution availableBook() {
        return CatalogResolution.available(
                new BookCatalogResponse(
                        "OL1W",
                        "Libro de prueba",
                        List.of("Autora"),
                        null,
                        null,
                        null,
                        "https://example.com/book.jpg"
                )
        );
    }

    private Long elementCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM list_elements WHERE list_id = ?",
                Long.class,
                listId
        );
    }

    @Test
    void debeGuardarConsultarProtegerYQuitarElemento() throws Exception {
        when(catalog.resolve(anyString(), anyString(), anyString()))
                .thenReturn(availableBook());

        String path = "/api/profiles/me/lists/" + listId;

        String body = """
                {
                  "elementType": "BOOK",
                  "referenceId": "OL1W"
                }
                """;

        mvc.perform(post(path + "/elements")
                        .with(jwt().jwt(token ->
                                token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mvc.perform(get(path)
                        .with(jwt().jwt(token ->
                                token.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements[0].content.title")
                        .value("Libro de prueba"));

        mvc.perform(post(path + "/elements")
                        .with(jwt().jwt(token ->
                                token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

        mvc.perform(get(path)
                        .with(jwt().jwt(token ->
                                token.subject(otherUserId.toString()))))
                .andExpect(status().isNotFound());

        assertEquals(Long.valueOf(1), elementCount());

        UUID elementId = jdbc.queryForObject(
                "SELECT id FROM list_elements WHERE list_id = ?",
                UUID.class,
                listId
        );

        mvc.perform(delete(path + "/elements/" + elementId)
                        .with(jwt().jwt(token ->
                                token.subject(userId.toString()))))
                .andExpect(status().isNoContent());

        assertEquals(Long.valueOf(0), elementCount());
    }

    @Test
    void dosIntentosSimultaneosDebenGuardarUnSoloElemento()
            throws Exception {
        var barrier = new CyclicBarrier(2);

        when(catalog.resolve("BOOK", "OL1W", "token-prueba"))
                .thenAnswer(invocation -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return availableBook();
                });

        Callable<Integer> add = () -> {
            try {
                service.addElement(
                        userId,
                        listId,
                        new AddListElementRequest("BOOK", "OL1W"),
                        "token-prueba"
                );

                return 201;
            } catch (ListElementAlreadyExistsException exception) {
                return 409;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(add);
            var second = executor.submit(add);

            var results = new ArrayList<>(List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)
            ));

            results.sort(Integer::compareTo);

            assertEquals(List.of(201, 409), results);
            assertEquals(Long.valueOf(1), elementCount());
        }
    }
}