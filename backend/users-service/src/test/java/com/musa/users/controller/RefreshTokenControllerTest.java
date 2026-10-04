package com.musa.users.controller;

import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.service.JwtService;
import com.musa.users.service.RefreshTokenService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RefreshTokenController.class)
class RefreshTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void refreshToken_debeResponder200ConNuevosTokens() throws Exception {

        // ARRANGE: Simulación de renovación de tokens con token válido
        AuthResponseDto mockResponse = new AuthResponseDto(
                "new-access-token",
                "new-refresh-token",
                UUID.randomUUID(),
                "Ana López",
                "USER"
        );
        when(refreshTokenService.refreshToken(any())).thenReturn(mockResponse);

        String json = """
                {
                    "refreshToken": "valid-refresh-token"
                }
                """;

        // ACT: Envío de solicitud de refresco
        // ASSERT: Validación de HTTP 200 OK y entrega de nuevos tokens
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }
}
