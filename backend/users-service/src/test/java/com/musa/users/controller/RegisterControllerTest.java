package com.musa.users.controller;

import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.JwtService;
import com.musa.users.service.RegisterService;

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

@WebMvcTest(RegisterController.class)
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterService registerService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void register_debeResponder201ConMensajeExitoso() throws Exception {

        // ARRANGE: Preparación del mock del servicio y datos del nuevo usuario
        when(registerService.register(any())).thenReturn(new MessageResponseDto("Registro exitoso."));

        String json = """
                {
                    "fullName": "Ana López",
                    "username": "ana_lopez",
                    "email": "ana@usi.com",
                    "password": "Password123!",
                    "confirmPassword": "Password123!",
                    "roleName": "USER"
                }
                """;

        // ACT: Ejecución de la petición POST
        // ASSERT: Validación del código HTTP 201 y mensaje de éxito
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Registro exitoso."));
    }

    @Test
    @WithMockUser
    void register_debeRechazarUsernameAusenteOInvalido() throws Exception {
        String base = "{\"fullName\":\"Ana\",\"email\":\"ana@example.com\","
                + "\"password\":\"Password123!\",\"confirmPassword\":\"Password123!\",\"roleName\":\"USER\"";
        for (String suffix : new String[]{"}", ",\"username\":\"no espacios\"}"}) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON).content(base + suffix))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.username").exists());
        }
        org.mockito.Mockito.verifyNoInteractions(registerService);
    }
}

