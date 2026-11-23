package com.musa.chat;

import com.musa.chat.client.UsersClient;
import com.musa.chat.config.JwtConfig;
import com.musa.chat.exception.*;
import com.musa.chat.dto.request.CreatePrivateConversationRequest;
import com.musa.chat.entity.*;
import com.musa.chat.repository.*;
import com.musa.chat.service.impl.ConversationServiceImpl;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PrivateEndpointBlockTest {
    private final UUID sender = UUID.randomUUID();
    private final UUID recipient = UUID.randomUUID();
    private final byte[] key = new byte[64];

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    private String signedToken(String issuer, String audience, String subject, Instant expiration) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer).audience(audience).subject(subject)
                .issueTime(Date.from(Instant.now().minusSeconds(10)))
                .expirationTime(Date.from(expiration)).build();
        SignedJWT token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        token.sign(new MACSigner(key));
        return token.serialize();
    }

    private JwtDecoder decoder() { return new JwtConfig().jwtDecoder(Base64.getEncoder().encodeToString(key)); }

    @Test
    void acceptsMusaToken() throws Exception {
        String token = signedToken("musa-auth", "musa-api", sender.toString(), Instant.now().plusSeconds(300));
        assertEquals(sender.toString(), decoder().decode(token).getSubject());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String token = signedToken("musa-auth", "musa-api", sender.toString(), Instant.now().minusSeconds(60));
        assertThrows(JwtException.class, () -> decoder().decode(token));
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        String token = signedToken("other", "musa-api", sender.toString(), Instant.now().plusSeconds(300));
        assertThrows(JwtException.class, () -> decoder().decode(token));
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        String token = signedToken("musa-auth", "other", sender.toString(), Instant.now().plusSeconds(300));
        assertThrows(JwtException.class, () -> decoder().decode(token));
    }

    @Test
    void rejectsNonUuidSubject() throws Exception {
        String token = signedToken("musa-auth", "musa-api", "email@example.com", Instant.now().plusSeconds(300));
        assertThrows(JwtException.class, () -> decoder().decode(token));
    }

    @Test
    void rejectsOtherSigningKey() throws Exception {
        String token = signedToken("musa-auth", "musa-api", sender.toString(), Instant.now().plusSeconds(300));
        byte[] otherKey = new byte[64]; Arrays.fill(otherKey, (byte) 1);
        var otherDecoder = new JwtConfig().jwtDecoder(Base64.getEncoder().encodeToString(otherKey));
        assertThrows(JwtException.class, () -> otherDecoder.decode(token));
    }

    private void authenticate() {
        Jwt jwt = Jwt.withTokenValue("test-access-token").header("alg", "HS512")
                .subject(sender.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private UsersClient client(MockRestServiceServer[] holder) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://users.test");
        holder[0] = MockRestServiceServer.bindTo(builder).build();
        return new UsersClient(builder.build());
    }

    @Test
    void forwardsTokenAndAcceptsActiveParticipants() {
        authenticate();
        var holder = new MockRestServiceServer[1]; var client = client(holder);
        holder[0].expect(requestTo("http://users.test/api/v1/users/usernames?ids=" + sender + "&ids=" + recipient))
                .andExpect(header("Authorization", "Bearer test-access-token"))
                .andRespond(withSuccess("{\"" + sender + "\":\"liz\",\"" + recipient + "\":\"ana\"}", MediaType.APPLICATION_JSON));
        assertDoesNotThrow(() -> client.requireActiveParticipants(sender, recipient));
        holder[0].verify();
    }

    @Test
    void unavailableRecipientIsNotTreatedAsValid() {
        authenticate();
        var holder = new MockRestServiceServer[1]; var client = client(holder);
        holder[0].expect(anything()).andRespond(withSuccess("{\"" + sender + "\":\"liz\"}", MediaType.APPLICATION_JSON));
        assertThrows(RecipientUnavailableException.class, () -> client.requireActiveParticipants(sender, recipient));
    }

    @Test
    void usersOutageIsNotMisreportedAsMissingRecipient() {
        authenticate();
        var holder = new MockRestServiceServer[1]; var client = client(holder);
        holder[0].expect(anything()).andRespond(withServerError());
        assertThrows(UsersServiceUnavailableException.class, () -> client.requireActiveParticipants(sender, recipient));
    }

    @Test
    void rejectedSessionRemainsUnauthorized() {
        authenticate();
        var holder = new MockRestServiceServer[1]; var client = client(holder);
        holder[0].expect(anything()).andRespond(withUnauthorizedRequest());
        assertThrows(BadCredentialsException.class, () -> client.requireActiveParticipants(sender, recipient));
    }

    @Test
    void cannotSubstituteSenderIdentity() {
        authenticate();
        var holder = new MockRestServiceServer[1]; var client = client(holder);
        assertThrows(BadCredentialsException.class, () -> client.requireActiveParticipants(UUID.randomUUID(), recipient));
        holder[0].verify();
    }

    @Test
    void existingConversationUsesLockBeforeLookupAndDoesNotCreate() {
        var conversations = mock(ConversationRepository.class);
        var participants = mock(ConversationParticipantRepository.class);
        var lock = mock(PrivateConversationLock.class);
        var users = mock(UsersClient.class);
        var service = new ConversationServiceImpl(conversations, participants, lock, users);
        var conversation = new Conversation(); conversation.setId(UUID.randomUUID());
        conversation.setCreatedAt(Instant.now());
        when(conversations.findPrivateConversationBetween(sender, recipient, ConversationType.PRIVATE))
                .thenReturn(Optional.of(conversation));
        when(participants.findByConversationIdOrderByUserIdAsc(conversation.getId())).thenReturn(List.of());
        assertEquals(conversation.getId(), service.createPrivateConversation(sender,
                new CreatePrivateConversationRequest(recipient)).id());
        var order = inOrder(users, lock, conversations);
        order.verify(users).requireActiveParticipants(sender, recipient);
        order.verify(lock).acquire(sender, recipient);
        order.verify(conversations).findPrivateConversationBetween(sender, recipient, ConversationType.PRIVATE);
        verify(conversations, never()).save(any());
        verify(participants, never()).saveAll(any());
    }

    @Test
    void selfConversationDoesNotCallUsersOrDatabase() {
        var conversations = mock(ConversationRepository.class);
        var participants = mock(ConversationParticipantRepository.class);
        var lock = mock(PrivateConversationLock.class);
        var users = mock(UsersClient.class);
        var service = new ConversationServiceImpl(conversations, participants, lock, users);
        assertThrows(SelfConversationNotAllowedException.class, () -> service.createPrivateConversation(
                sender, new CreatePrivateConversationRequest(sender)));
        verifyNoInteractions(conversations, participants, lock, users);
    }

    @Test
    void outageResponseDoesNotExposeCause() {
        var handler = new GlobalExceptionHandler();
        var response = handler.usersUnavailable(new UsersServiceUnavailableException(
                new IllegalStateException("private-server-details")));
        assertEquals(503, response.getStatusCode().value());
        assertFalse(response.getBody().message().contains("private-server-details"));
        assertEquals("5", response.getHeaders().getFirst("Retry-After"));
    }
}
