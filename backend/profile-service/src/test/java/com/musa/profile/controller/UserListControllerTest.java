package com.musa.profile.controller;

import com.musa.profile.config.SecurityConfig;
import com.musa.profile.dto.AddListElementRequest;
import com.musa.profile.dto.ListElementResponse;
import com.musa.profile.dto.ResolvedListElementResponse;
import com.musa.profile.dto.UserListDetailResponse;
import com.musa.profile.dto.catalog.BookCatalogResponse;
import com.musa.profile.dto.catalog.CatalogResolution;
import com.musa.profile.exception.ListElementAlreadyExistsException;
import com.musa.profile.exception.UserListNotFoundException;
import com.musa.profile.service.UserListService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserListController.class)
@Import(SecurityConfig.class)
class UserListControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserListService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RequestPostProcessor authenticated(UUID userId) {
        Jwt token = Jwt.withTokenValue("token-prueba")
                .header("alg", "none")
                .subject(userId.toString())
                .build();

        return jwt().jwt(token);
    }

    private String elementBody() {
        return """
                {
                  "elementType": "BOOK",
                  "referenceId": "OL1W"
                }
                """;
    }

    @Test
    void debeExigirTokenParaConsultarAgregarYQuitar() throws Exception {
        UUID listId = UUID.randomUUID();
        UUID elementId = UUID.randomUUID();

        mvc.perform(get("/api/profiles/me/lists/" + listId))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/profiles/me/lists/" + listId + "/elements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementBody()))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete(
                        "/api/profiles/me/lists/" + listId
                                + "/elements/" + elementId
                ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void debeConsultarConUuidDelTokenYDevolverContenido() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        var book = new BookCatalogResponse(
                "OL1W",
                "Libro de prueba",
                List.of("Autora"),
                null,
                null,
                null,
                "https://example.com/book.jpg"
        );

        var element = new ResolvedListElementResponse(
                UUID.randomUUID(),
                "BOOK",
                "OL1W",
                CatalogResolution.Status.AVAILABLE,
                book
        );

        when(service.getListDetail(userId, listId, "token-prueba"))
                .thenReturn(new UserListDetailResponse(
                        listId,
                        "Mi lista",
                        null,
                        OffsetDateTime.now(ZoneOffset.UTC),
                        List.of(element)
                ));

        mvc.perform(get("/api/profiles/me/lists/" + listId)
                        .with(authenticated(userId))
                        .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements[0].resolutionStatus")
                        .value("AVAILABLE"))
                .andExpect(jsonPath("$.elements[0].content.coverUrl")
                        .value("https://example.com/book.jpg"));

        verify(service).getListDetail(
                userId, listId, "token-prueba"
        );
    }

    @Test
    void debeAgregarConUuidYTokenAutenticados() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(service.addElement(
                eq(userId),
                eq(listId),
                any(AddListElementRequest.class),
                eq("token-prueba")
        )).thenReturn(new ListElementResponse(
                UUID.randomUUID(),
                "BOOK",
                "OL1W"
        ));

        mvc.perform(post("/api/profiles/me/lists/" + listId + "/elements")
                        .with(authenticated(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceId").value("OL1W"));

        var captor = ArgumentCaptor.forClass(AddListElementRequest.class);

        verify(service).addElement(
                eq(userId),
                eq(listId),
                captor.capture(),
                eq("token-prueba")
        );

        assertEquals("BOOK", captor.getValue().elementType());
    }

    @Test
    void debeRechazarTiposNoAdmitidos() throws Exception {
        UUID listId = UUID.randomUUID();

        mvc.perform(post("/api/profiles/me/lists/" + listId + "/elements")
                        .with(authenticated(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elementType": "ALBUM",
                                  "referenceId": "album-test"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void debeResponder404ParaListaAjenaONoExistente() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(service.getListDetail(userId, listId, "token-prueba"))
                .thenThrow(new UserListNotFoundException(listId));

        mvc.perform(get("/api/profiles/me/lists/" + listId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void debeResponder503SiNoPuedeValidarContenido() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(service.addElement(
                eq(userId),
                eq(listId),
                any(AddListElementRequest.class),
                eq("token-prueba")
        )).thenThrow(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Catálogo no disponible"
        ));

        mvc.perform(post("/api/profiles/me/lists/" + listId + "/elements")
                        .with(authenticated(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementBody()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void debeResponder409ParaDuplicados() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();

        when(service.addElement(
                eq(userId),
                eq(listId),
                any(AddListElementRequest.class),
                eq("token-prueba")
        )).thenThrow(new ListElementAlreadyExistsException());

        mvc.perform(post("/api/profiles/me/lists/" + listId + "/elements")
                        .with(authenticated(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void debeQuitarElementoUsandoUuidAutenticado() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID listId = UUID.randomUUID();
        UUID elementId = UUID.randomUUID();

        mvc.perform(delete(
                        "/api/profiles/me/lists/" + listId
                                + "/elements/" + elementId
                ).with(authenticated(userId)))
                .andExpect(status().isNoContent());

        verify(service).removeElement(userId, listId, elementId);
    }
}