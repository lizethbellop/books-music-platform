package com.musa.users.controller;

import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.service.JwtService;
import com.musa.users.service.LoginService;

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

@WebMvcTest(LoginController.class)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void login_debeResponder200ConTokensYDatosUsuario() throws Exception {

        // ARRANGE: Preparación del mock de autenticación exitosa
        AuthResponseDto mockResponse = new AuthResponseDto(
                "access-token-demo",
                "refresh-token-demo",
                UUID.randomUUID(),
                "Ana López",
                "USER"
        );
        when(loginService.login(any())).thenReturn(mockResponse);

        String json = """
                {
                    "email": "ana@usi.com",
                    "password": "Password123!"
                }
                """;

        // ACT: Envío de credenciales al endpoint
        // ASSERT: Validación de respuesta HTTP 200 y tokens en el JSON
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("access-token-demo"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-demo"))
                .andExpect(jsonPath("$.fullName").value("Ana López"))
                .andExpect(jsonPath("$.roleName").value("USER"));
    }
}