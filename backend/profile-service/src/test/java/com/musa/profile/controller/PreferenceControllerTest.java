package com.musa.profile.controller;

import com.musa.profile.config.SecurityConfig;
import com.musa.profile.dto.PreferencesResponse;
import com.musa.profile.service.PreferenceService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PreferenceController.class)
@Import(SecurityConfig.class)
class PreferenceControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PreferenceService service;
    @MockitoBean JwtDecoder decoder;

    @Test void readsPreferencesWithTokenWithoutLegacyHeader() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.getOwnPreferences(userId, "token")).thenReturn(new PreferencesResponse(List.of()));
        mvc.perform(get("/api/profiles/me/preferences")
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
        verify(service).getOwnPreferences(userId, "token");
    }

    @Test void rejectsMissingToken() throws Exception {
        mvc.perform(get("/api/profiles/me/preferences"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void removesPreferenceWithAuthenticatedUuid() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID elementId = UUID.randomUUID();
        mvc.perform(delete("/api/profiles/me/preferences/" + elementId)
                        .with(jwt().jwt(token -> token.subject(userId.toString()))))
                .andExpect(status().isNoContent());
        verify(service).removeElement(userId, elementId);
    }
}
