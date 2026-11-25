package com.musa.chat;

import com.musa.chat.config.JwtConfig;
import com.musa.chat.config.SecurityConfig;
import com.musa.chat.controller.ConversationController;
import com.musa.chat.controller.HealthController;
import com.musa.chat.dto.response.ConversationResponse;
import com.musa.chat.entity.ConversationType;
import com.musa.chat.exception.*;
import com.musa.chat.service.ConversationService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EndpointSecurityBlockTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private ConversationService service;
    private final UUID sender = UUID.randomUUID();
    private final UUID recipient = UUID.randomUUID();
    private static final String PATH = "/api/v1/chat/conversations/private";

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, ConversationController.class,
            HealthController.class, GlobalExceptionHandler.class})
    static class TestConfig {
        @Bean ConversationService conversationService() { return mock(ConversationService.class); }
        @Bean JwtDecoder jwtDecoder() {
            return new JwtConfig().jwtDecoder(Base64.getEncoder().encodeToString(new byte[64]));
        }
    }

    @BeforeEach
    void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class); context.refresh();
        service = context.getBean(ConversationService.class);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class)).build();
    }

    @AfterEach void close() { context.close(); }

    private String bearer() throws Exception {
        var claims = new JWTClaimsSet.Builder().subject(sender.toString())
                .issuer("musa-auth").audience("musa-api")
                .expirationTime(Date.from(Instant.now().plusSeconds(300))).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        jwt.sign(new MACSigner(new byte[64]));
        return "Bearer " + jwt.serialize();
    }

    @Test void healthIsPublic() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test void missingTokenIs401() throws Exception {
        mvc.perform(post(PATH).contentType("application/json")
                .content("{\"recipientId\":\"" + recipient + "\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(service);
    }

    @Test void invalidTokenIs401() throws Exception {
        mvc.perform(post(PATH).header("Authorization", "Bearer invalid")
                .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void nullRecipientIs400() throws Exception {
        mvc.perform(post(PATH).header("Authorization", bearer())
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test void malformedUuidIs400() throws Exception {
        mvc.perform(post(PATH).header("Authorization", bearer())
                .contentType("application/json").content("{\"recipientId\":\"wrong\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test void usesSignedSubjectAndIgnoresForgedIdentityHeader() throws Exception {
        when(service.createPrivateConversation(eq(sender), any())).thenReturn(
                new ConversationResponse(UUID.randomUUID(), ConversationType.PRIVATE,
                        Instant.now(), List.of(sender, recipient)));
        mvc.perform(post(PATH).header("Authorization", bearer())
                .header("X-User-Id", UUID.randomUUID().toString())
                .contentType("application/json")
                .content("{\"recipientId\":\"" + recipient + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("PRIVATE"));
        verify(service).createPrivateConversation(eq(sender), argThat(r -> recipient.equals(r.recipientId())));
    }

    @Test void selfConversationIs400() throws Exception {
        when(service.createPrivateConversation(eq(sender), any())).thenThrow(new SelfConversationNotAllowedException());
        mvc.perform(post(PATH).header("Authorization", bearer()).contentType("application/json")
                .content("{\"recipientId\":\"" + sender + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SELF_CONVERSATION_NOT_ALLOWED"));
    }

    @Test void busyConversationIs503WithoutCause() throws Exception {
        when(service.createPrivateConversation(eq(sender), any())).thenThrow(
                new ConversationBusyException(new RuntimeException("internal-secret")));
        mvc.perform(post(PATH).header("Authorization", bearer()).contentType("application/json")
                .content("{\"recipientId\":\"" + recipient + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.code").value("CONVERSATION_BUSY"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("internal-secret"))));
    }
}
