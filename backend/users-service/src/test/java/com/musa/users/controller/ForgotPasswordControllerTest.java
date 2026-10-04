package com.musa.users.controller;

import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.ForgotPasswordService;
import com.musa.users.service.JwtService;

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

@WebMvcTest(ForgotPasswordController.class)
class ForgotPasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ForgotPasswordService forgotPasswordService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void forgotPassword_debeResponder200ConMensajeExitoso() throws Exception {

        // ARRANGE: Simulación del envío de correo de recuperación
        when(forgotPasswordService.sendResetPasswordEmail(any()))
                .thenReturn(new MessageResponseDto("Correo de recuperación enviado exitosamente."));

        String json = """
                {
                    "email": "ana@usi.com"
                }
                """;

        // ACT: Envío del correo al endpoint
        // ASSERT: Validación de HTTP 200 OK y confirmación de envío
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Correo de recuperación enviado exitosamente."));
    }
}
