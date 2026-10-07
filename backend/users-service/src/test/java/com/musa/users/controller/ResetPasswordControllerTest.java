package com.musa.users.controller;

import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.JwtService;
import com.musa.users.service.ResetPasswordService;

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

@WebMvcTest(ResetPasswordController.class)
class ResetPasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResetPasswordService resetPasswordService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void resetPassword_debeResponder200ConMensajeExitoso() throws Exception {

        // ARRANGE: Simulación del cambio de contraseña exitoso
        when(resetPasswordService.resetPassword(any()))
                .thenReturn(new MessageResponseDto("Contraseña restablecida exitosamente."));

        String json = """
                {
                    "token": "valid-reset-token",
                    "newPassword": "NewPassword123!",
                    "confirmNewPassword": "NewPassword123!"
                }
                """;

        // ACT: Envío del token y nueva contraseña al endpoint
        // ASSERT: Validación de HTTP 200 OK y confirmación del cambio
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Contraseña restablecida exitosamente."));
    }
}
