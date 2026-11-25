package com.musa.chat.client;
import com.musa.chat.exception.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.util.*;
@Component
public class UsersClient {
    private final RestClient restClient;
    public UsersClient(@Qualifier("usersRestClient") RestClient restClient) { this.restClient = restClient; }
    public void requireActiveParticipants(UUID currentUserId, UUID recipientId) {
        requireActiveUsers(currentUserId, List.of(currentUserId, recipientId));
    }
    public void requireActiveUsers(UUID currentUserId, Collection<UUID> ids) {
        Map<String, String> active = findActiveUsers(currentUserId, ids);
        if (ids.stream().anyMatch(id -> !active.containsKey(id.toString()))) throw new RecipientUnavailableException();
    }
    public Map<String, String> findActiveUsers(UUID currentUserId, Collection<UUID> ids) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()
                || !currentUserId.toString().equals(jwt.getToken().getSubject()))
            throw new BadCredentialsException("Identidad no válida.");
        if (ids.isEmpty() || ids.size() > 100) throw new InvalidChatRequestException("Cantidad de usuarios no válida.");
        Map<String, String> users;
        try {
            users = restClient.get().uri(builder -> builder.path("/api/v1/users/usernames")
                    .queryParam("ids", ids.toArray()).build())
                    .headers(headers -> headers.setBearerAuth(jwt.getToken().getTokenValue()))
                    .retrieve().body(new ParameterizedTypeReference<Map<String, String>>() {});
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401) throw new BadCredentialsException("Sesión no válida.", exception);
            throw new UsersServiceUnavailableException(exception);
        } catch (RestClientException exception) { throw new UsersServiceUnavailableException(exception); }
        if (users == null) throw new UsersServiceUnavailableException(new IllegalStateException("Respuesta vacía de users-service"));
        if (!users.containsKey(currentUserId.toString())) throw new BadCredentialsException("Usuario autenticado no disponible.");
        return users;
    }
}
