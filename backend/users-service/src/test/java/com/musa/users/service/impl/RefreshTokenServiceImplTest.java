package com.musa.users.service.impl;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.entity.Role;
import com.musa.users.entity.User;
import com.musa.users.exception.InvalidTokenException;
import com.musa.users.exception.AccountDisabledException;
import com.musa.users.session.RefreshSession;
import com.musa.users.session.RefreshSessionStore;
import com.musa.users.repository.UserRepository;
import com.musa.users.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshSessionStore refreshSessionStore;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private TokenRequestDto tokenRequest;
    private User user;
    private UUID userId;
    private RefreshSession session;

    @BeforeEach
    void setUp() {
        session = new RefreshSession("ana@usi.com", Instant.now().plusSeconds(3600), true);

        tokenRequest = new TokenRequestDto("refresh-token-valido-123");

        userId = UUID.randomUUID();

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        user = new User();
        user.setId(userId);
        user.setFullName("Ana López");
        user.setUsername("ana_lopez");
        user.setEmail("ana@usi.com");
        user.setRole(role);
        user.setIsActive(true);
    }

    @Test
    @DisplayName("Debe lanzar InvalidTokenException si el refresh token no existe en Redis")
    void refreshToken_debeLanzarExcepcion_siTokenNoExisteEnRedis() {
        // Arrange: simula una sesión inexistente o vencida
        when(refreshSessionStore.find(tokenRequest.refreshToken())).thenReturn(Optional.empty());

        // Act & Assert: Verifica que la falta de token en caché dispare la excepción de token inválido
        assertThrows(InvalidTokenException.class, () -> refreshTokenService.refreshToken(tokenRequest));

        // Verify: Confirma que la ejecución se detuvo y no consultó repositorios ni servicios
        verify(refreshSessionStore).find(tokenRequest.refreshToken());
        verifyNoInteractions(userRepository, userDetailsService, jwtService);
    }

    @Test
    @DisplayName("Debe lanzar InvalidTokenException si el usuario asociado al token no existe")
    void refreshToken_debeLanzarExcepcion_siUsuarioNoExiste() {
        // Arrange: existe la sesión, pero el usuario ya no existe
        when(refreshSessionStore.find(tokenRequest.refreshToken())).thenReturn(Optional.of(session));
        when(userRepository.findByEmailWithRole("ana@usi.com")).thenReturn(Optional.empty());

        // Act & Assert: Verifica que se lance la excepción cuando el usuario fue eliminado
        assertThrows(InvalidTokenException.class, () -> refreshTokenService.refreshToken(tokenRequest));

        // Verify: Valida la consulta al repositorio y asegura que no se generó ningún JWT
        verify(userRepository).findByEmailWithRole("ana@usi.com");
        verifyNoInteractions(userDetailsService, jwtService);
    }

    @Test
    @DisplayName("Debe renovar el Access Token y rotar el refresh token sin extender la sesión")
    void refreshToken_debeRenovarAccessTokenYConservarExpiracion_siTokenEsValido() {
        // Arrange: Simula la presencia del token en Redis, usuario existente y generación exitosa del JWT
        UserDetails userDetailsMock = mock(UserDetails.class);

        when(refreshSessionStore.find(tokenRequest.refreshToken())).thenReturn(Optional.of(session));
        when(userRepository.findByEmailWithRole("ana@usi.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("ana@usi.com")).thenReturn(userDetailsMock);
        when(jwtService.generateAccessToken(
                userDetailsMock, userId, session.sessionExpiresAt()))
                .thenReturn("NUEVO_ACCESS_TOKEN");
        Instant accessExpiresAt = Instant.now().plusSeconds(900);
        when(jwtService.getAccessTokenExpiresAt("NUEVO_ACCESS_TOKEN")).thenReturn(accessExpiresAt);
        when(refreshSessionStore.rotate(tokenRequest.refreshToken(), session))
                .thenReturn(Optional.of("NUEVO_REFRESH_TOKEN"));

        // Act: Ejecución del refresco de token
        AuthResponseDto response = refreshTokenService.refreshToken(tokenRequest);

        // Assert: Agrupa comprobaciones sobre el DTO resultante
        assertAll(
                () -> assertEquals("NUEVO_ACCESS_TOKEN", response.accessToken()),
                () -> assertEquals("NUEVO_REFRESH_TOKEN", response.refreshToken()),
                () -> assertEquals(session.sessionExpiresAt(), response.sessionExpiresAt()),
                () -> assertEquals(accessExpiresAt, response.accessTokenExpiresAt()),
                () -> assertEquals(userId, response.userId()),
                () -> assertEquals("Ana López", response.fullName()),
                () -> assertEquals("USER", response.roleName()),
                () -> assertEquals("ana_lopez", response.username())
        );

        // Verify: se rota conservando los datos de la sesión original
        verify(refreshSessionStore).rotate(tokenRequest.refreshToken(), session);
    }
    @Test
    @DisplayName("Debe revocar la sesión si la cuenta está desactivada")
    void refreshToken_debeRechazarCuentaDesactivada() {
        user.setIsActive(false);
        when(refreshSessionStore.find(tokenRequest.refreshToken())).thenReturn(Optional.of(session));
        when(userRepository.findByEmailWithRole("ana@usi.com")).thenReturn(Optional.of(user));

        assertThrows(AccountDisabledException.class, () -> refreshTokenService.refreshToken(tokenRequest));
        verify(refreshSessionStore).revoke(tokenRequest.refreshToken());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Debe rechazar la renovación si otra solicitud ya consumió el token")
    void refreshToken_debeRechazarTokenConsumidoDuranteRotacion() {
        UserDetails userDetailsMock = mock(UserDetails.class);
        when(refreshSessionStore.find(tokenRequest.refreshToken())).thenReturn(Optional.of(session));
        when(userRepository.findByEmailWithRole("ana@usi.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("ana@usi.com")).thenReturn(userDetailsMock);
        when(jwtService.generateAccessToken(
                userDetailsMock, userId, session.sessionExpiresAt()))
                .thenReturn("NUEVO_ACCESS_TOKEN");
        when(refreshSessionStore.rotate(tokenRequest.refreshToken(), session)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> refreshTokenService.refreshToken(tokenRequest));
        verify(jwtService, never()).getAccessTokenExpiresAt(anyString());
    }
}
