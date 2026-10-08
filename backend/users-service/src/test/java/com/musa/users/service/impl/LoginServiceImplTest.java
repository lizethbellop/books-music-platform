package com.musa.users.service.impl;

import com.musa.users.dto.request.LoginRequestDto;
import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.entity.Role;
import com.musa.users.entity.User;
import com.musa.users.exception.AccountDisabledException;
import com.musa.users.exception.InvalidCredentialsException;
import com.musa.users.session.RefreshSession;
import com.musa.users.session.RefreshSessionStore;
import org.mockito.ArgumentCaptor;
import com.musa.users.repository.UserRepository;
import com.musa.users.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RefreshSessionStore refreshSessionStore;

    @InjectMocks
    private LoginServiceImpl loginService;

    private LoginRequestDto loginRequest;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        // Asignación de propiedades @Value inyectadas dinámicamente
        ReflectionTestUtils.setField(loginService, "refreshTokenExpiration", 604800000L);
        ReflectionTestUtils.setField(loginService, "maxFailedAttempts", 3);
        ReflectionTestUtils.setField(loginService, "lockTimeMinutes", 5L);


        loginRequest = new LoginRequestDto(
                "ana@usi.com",
                "Password123!",
                false
        );

        userId = UUID.randomUUID();
        Role role = new Role();

        role.setId(1L);
        role.setName("USER");

        user = new User();
        user.setId(userId);
        user.setFullName("Ana López");
        user.setUsername("ana_lopez");
        user.setEmail("ana@usi.com");
        user.setPasswordHash("HASH_ENCRIPTADO");
        user.setIsActive(true);
        user.setRole(role);
    }

    @Test
    @DisplayName("Debe lanzar InvalidCredentialsException si la cuenta está bloqueada en Redis")
    void login_debeLanzarExcepcion_siCuentaEstaBloqueadaEnRedis() {
        // Arrange
        when(redisTemplate.hasKey("lock:ana@usi.com")).thenReturn(true);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> loginService.login(loginRequest));

        // Verify: No debe consultar base de datos si la clave de bloqueo existe
        verify(redisTemplate).hasKey("lock:ana@usi.com");
        verifyNoInteractions(userRepository, passwordEncoder, jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Debe lanzar InvalidCredentialsException si el usuario no existe en la base de datos")
    void login_debeLanzarExcepcion_siUsuarioNoExiste() {
        // Arrange
        when(redisTemplate.hasKey("lock:ana@usi.com")).thenReturn(false);
        when(userRepository.findByEmailWithRole("ana@usi.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> loginService.login(loginRequest));

        verify(userRepository).findByEmailWithRole("ana@usi.com");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Debe lanzar AccountDisabledException si la cuenta está desactivada")
    void login_debeLanzarExcepcion_siCuentaEstaDesactivada() {
        // Arrange
        user.setIsActive(false);
        when(redisTemplate.hasKey("lock:ana@usi.com")).thenReturn(false);
        when(userRepository.findByEmailWithRole("ana@usi.com")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(AccountDisabledException.class, () -> loginService.login(loginRequest));

        verify(userRepository).findByEmailWithRole("ana@usi.com");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Debe registrar intento fallido y lanzar InvalidCredentialsException si la contraseña no coincide")
    void login_debeRegistrarIntentoFallidoYLanzarExcepcion_siPasswordEsIncorrecta() {
        // Arrange
        when(redisTemplate.hasKey("lock:ana@usi.com")).thenReturn(false);
        when(userRepository.findByEmailWithRole("ana@usi.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "HASH_ENCRIPTADO")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("attempts:ana@usi.com")).thenReturn(1L);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> loginService.login(loginRequest));

        // Verify: Se incrementa el contador y se le asigna tiempo de expiración
        verify(valueOperations).increment("attempts:ana@usi.com");
        verify(redisTemplate).expire(eq("attempts:ana@usi.com"), any(Duration.class));
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Debe bloquear la cuenta cuando se alcanzan los intentos fallidos máximos")
    void login_debeBloquearCuenta_siAlcanzaMaximoDeIntentosFallidos() {
        // Arrange
        when(redisTemplate.hasKey("lock:ana@usi.com")).thenReturn(false);
        when(userRepository.findByEmailWithRole("ana@usi.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "HASH_ENCRIPTADO")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("attempts:ana@usi.com")).thenReturn(3L); // Igual a maxFailedAttempts

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> loginService.login(loginRequest));

        // Verify: Se guarda la clave de bloqueo "LOCKED" y se limpia la de intentos
        verify(valueOperations).set(eq("lock:ana@usi.com"), eq("LOCKED"), any(Duration.class));
        verify(redisTemplate).delete("attempts:ana@usi.com");
    }

    @Test
    @DisplayName("Debe autenticar correctamente, limpiar intentos y retornar AuthResponseDto con Tokens")
    void login_debeAutenticarYDevolverTokens_siCredencialesSonValidas() {
        // Arrange
        UserDetails userDetailsMock = mock(UserDetails.class);

        when(redisTemplate.hasKey("lock:ana@usi.com")).thenReturn(false);
        when(userRepository.findByEmailWithRole("ana@usi.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "HASH_ENCRIPTADO")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("ana@usi.com")).thenReturn(userDetailsMock);
        when(jwtService.generateAccessToken(
                eq(userDetailsMock), eq(userId), any(Instant.class)))
                .thenReturn("TOKEN_ACCESS_SIMULADO");
        Instant accessExpiresAt = Instant.now().plusSeconds(900);
        when(jwtService.getAccessTokenExpiresAt("TOKEN_ACCESS_SIMULADO")).thenReturn(accessExpiresAt);
        when(refreshSessionStore.create(any(RefreshSession.class))).thenReturn("REFRESH_SIMULADO");

        // Act
        Instant before = Instant.ofEpochMilli(System.currentTimeMillis());
        AuthResponseDto response = loginService.login(loginRequest);
        Instant after = Instant.ofEpochMilli(System.currentTimeMillis());

        // Assert
        assertAll(
                () -> assertEquals("TOKEN_ACCESS_SIMULADO", response.accessToken()),
                () -> assertEquals("REFRESH_SIMULADO", response.refreshToken()),
                () -> assertEquals(accessExpiresAt, response.accessTokenExpiresAt()),
                () -> assertEquals(userId, response.userId()),
                () -> assertEquals("Ana López", response.fullName()),
                () -> assertEquals("USER", response.roleName()),
                () -> assertEquals("ana_lopez", response.username())
        );

        // Verify: Se limpia el historial de intentos, se actualiza última fecha de login y se guarda el Refresh Token en Redis
        verify(redisTemplate).delete("attempts:ana@usi.com");
        verify(userRepository).save(user);
        ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(refreshSessionStore).create(sessionCaptor.capture());
        RefreshSession session = sessionCaptor.getValue();
        assertEquals(response.sessionExpiresAt(), session.sessionExpiresAt());
        assertFalse(session.rememberMe());
        assertFalse(session.sessionExpiresAt().isBefore(before.plusSeconds(604800)));
        assertFalse(session.sessionExpiresAt().isAfter(after.plusSeconds(604800)));
    }
    @Test
    @DisplayName("Debe guardar la opción Recordarme elegida por el usuario")
    void login_debeGuardarRecordarme_siUsuarioLoSelecciona() {
        // Arrange
        UserDetails userDetailsMock = mock(UserDetails.class);
        when(userRepository.findByEmailWithRole("ana@usi.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "HASH_ENCRIPTADO")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("ana@usi.com")).thenReturn(userDetailsMock);
        when(jwtService.generateAccessToken(
                eq(userDetailsMock), eq(userId), any(Instant.class)))
                .thenReturn("TOKEN_ACCESS_SIMULADO");
        when(jwtService.getAccessTokenExpiresAt("TOKEN_ACCESS_SIMULADO"))
                .thenReturn(Instant.now().plusSeconds(900));
        when(refreshSessionStore.create(any(RefreshSession.class))).thenReturn("REFRESH_SIMULADO");

        // Act
        loginService.login(new LoginRequestDto("ana@usi.com", "Password123!", true));

        // Assert: guardar la decisión sin depender de Redis real
        verify(refreshSessionStore).create(argThat(session -> session.rememberMe()));
    }
}
