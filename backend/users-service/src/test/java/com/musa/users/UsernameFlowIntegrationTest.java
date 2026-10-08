package com.musa.users;

import com.musa.users.repository.UserRepository;
import com.musa.users.service.EmailService;
import com.musa.users.session.RefreshSessionStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "MUSA_USERNAME_DB_TEST", matches = "true")
@SpringBootTest(properties = {
        "spring.config.import=optional:file:.env[.properties]",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/BDAuth",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@Transactional
class UsernameFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RefreshSessionStore sessions;
    // Prueba la integración sin enviar correos reales.
    @MockitoBean EmailService email;

    @Test void registersPersistsAndRenewsUsernameWithoutChangingIdentityOrDeadline() throws Exception {
        String username = "Test_" + UUID.randomUUID().toString().replace("-", "");
        String normalized = username.toLowerCase(Locale.ROOT);
        String address = normalized + "@example.com";
        String password = "Password123!";
        List<String> refreshTokens = new ArrayList<>();
        try {
            String body = json.writeValueAsString(java.util.Map.of(
                    "fullName", "Prueba temporal", "username", username, "email", address,
                    "password", password, "confirmPassword", password, "roleName", "USUARIO"));
            mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
            var saved = users.findByEmail(address).orElseThrow();
            assertEquals(normalized, saved.getUsername());
            assertTrue(users.existsByUsernameIgnoreCase(username));
            mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(body.replace(address, "other_" + address)))
                    .andExpect(status().isConflict());
            var login = json.readTree(mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(java.util.Map.of("email", address,
                                    "password", password, "rememberMe", true))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(normalized))
                    .andReturn().getResponse().getContentAsString());
            assertEquals(saved.getId().toString(), login.get("userId").asString());
            String original = login.get("refreshToken").asString();
            refreshTokens.add(original);
            mvc.perform(get("/api/v1/users/usernames").param("ids", saved.getId().toString()))
                    .andExpect(status().is4xxClientError());
            String directory = mvc.perform(get("/api/v1/users/usernames")
                            .param("ids", saved.getId().toString())
                            .header("Authorization", "Bearer " + login.get("accessToken").asString()))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertEquals(normalized, json.readTree(directory).get(saved.getId().toString()).asString());
            assertEquals(1, json.readTree(directory).size());
            mvc.perform(get("/api/v1/users/usernames")
                            .param("ids", String.join(",", java.util.Collections.nCopies(101, saved.getId().toString())))
                            .header("Authorization", "Bearer " + login.get("accessToken").asString()))
                    .andExpect(status().isBadRequest());
            var renewed = json.readTree(mvc.perform(post("/api/v1/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(java.util.Map.of("refreshToken", original))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(normalized))
                    .andReturn().getResponse().getContentAsString());
            String current = renewed.get("refreshToken").asString();
            refreshTokens.add(current);
            assertEquals(login.get("userId"), renewed.get("userId"));
            assertEquals(login.get("sessionExpiresAt"), renewed.get("sessionExpiresAt"));
            mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(java.util.Map.of("refreshToken", current))))
                    .andExpect(status().isOk());
            assertTrue(sessions.find(current).isEmpty());
        } finally {
            for (String token : refreshTokens) sessions.revoke(token);
        }
    }

    @Test void recoversPasswordThroughEmailCodeAndRejectsReusedCode() throws Exception {
        String username = "reset_" + UUID.randomUUID().toString().replace("-", "");
        String address = username + "@example.com";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("fullName", "Recuperación temporal",
                                "username", username, "email", address, "password", "Password123!",
                                "confirmPassword", "Password123!", "roleName", "USUARIO"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", address))))
                .andExpect(status().isOk());
        var code = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(email).sendPasswordResetEmail(org.mockito.ArgumentMatchers.eq(address), code.capture());
        String token = code.getValue();
        String verifyBody = json.writeValueAsString(java.util.Map.of("token", token));
        mvc.perform(post("/api/v1/auth/verify-token").contentType(MediaType.APPLICATION_JSON).content(verifyBody))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("token", token,
                                "newPassword", "ChangedPassword123!", "confirmNewPassword", "ChangedPassword123!"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/verify-token").contentType(MediaType.APPLICATION_JSON).content(verifyBody))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", address, "password", "Password123!"))))
                .andExpect(status().isUnauthorized());
        var login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", address,
                                "password", "ChangedPassword123!", "rememberMe", false))))
                .andExpect(status().isOk()).andReturn().getResponse();
        String refresh = json.readTree(login.getContentAsString()).get("refreshToken").asString();
        sessions.revoke(refresh);
    }
}
