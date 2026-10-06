package com.musa.users.controller;

import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.JwtService;
import com.musa.users.service.LogoutService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogoutController.class)
class LogoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogoutService logoutService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void logout_debeResponder200ConMensajeExitoso() throws Exception {

        // ARRANGE: Simulamos el cierre de sesión exitoso
        when(logoutService.logout(any())).thenReturn(new MessageResponseDto("Sesión cerrada exitosamente."));

        String json = """
                {
                    "refreshToken": "sample-refresh-token"
                }
                """;

        // ACT: Solicitud de revocación del token
        // ASSERT: Validación de HTTP 200 OK y mensaje del servidor
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Sesión cerrada exitosamente."));
    }
}
