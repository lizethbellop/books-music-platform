package com.musa.profile.config;

import com.musa.profile.entity.Profile;
import com.musa.profile.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProfileService profileService;

    @Test
    void debeRechazarPeticionSinToken() throws Exception {
        mvc.perform(get("/api/profiles/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(profileService);
    }

    @Test
    void debeRechazarTokenInvalido() throws Exception {
        mvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(profileService);
    }

    @Test
    void debeConsultarPerfilConUuidDelToken() throws Exception {
        UUID userId = UUID.randomUUID();

        when(profileService.getByUserId(userId))
                .thenReturn(new Profile(userId, false));

        mvc.perform(get("/api/profiles/me")
                        .with(jwt().jwt(token ->
                                token.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(userId.toString()));

        verify(profileService).getByUserId(userId);
    }

    @Test
    void debeIgnorarUsuarioEnviadoEnHeader() throws Exception {
        UUID authenticatedUser = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();

        when(profileService.getByUserId(authenticatedUser))
                .thenReturn(new Profile(authenticatedUser, false));

        mvc.perform(get("/api/profiles/me")
                        .header("X-User-Id", otherUser.toString())
                        .with(jwt().jwt(token ->
                                token.subject(authenticatedUser.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(authenticatedUser.toString()));

        verify(profileService).getByUserId(authenticatedUser);
        verify(profileService, never()).getByUserId(otherUser);
    }

    @Test
    void debeRechazarCreacionDePerfilSinToken() throws Exception {
        mvc.perform(post("/api/profiles/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(profileService);
    }

    @Test
    void debeCrearPerfilConUuidDelToken() throws Exception {
        UUID authenticatedUser = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();

        when(profileService.ensureOwnProfile(authenticatedUser))
                .thenReturn(new Profile(authenticatedUser, false));

        mvc.perform(post("/api/profiles/me")
                        .header("X-User-Id", otherUser.toString())
                        .with(jwt().jwt(token ->
                                token.subject(authenticatedUser.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(authenticatedUser.toString()));

        verify(profileService).ensureOwnProfile(authenticatedUser);
        verify(profileService, never()).ensureOwnProfile(otherUser);
    }
}