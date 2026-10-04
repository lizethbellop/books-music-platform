package com.musa.users.service.impl;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.entity.Role;
import com.musa.users.entity.User;
import com.musa.users.exception.InvalidTokenException;
import com.musa.users.exception.ResourceNotFoundException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

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

    @BeforeEach
    void setUp() {
        // Asignación de propiedad @Value inyectada dinámicamente mediante ReflectionTestUtils
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 2400000L);

        tokenRequest = new TokenRequestDto("refresh-token-valido-123");

        userId = UUID.randomUUID();

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");

        user = new User();
        user.setId(userId);
        user.setFullName("Ana López");
        user.setEmail("ana@usi.com");
        user.setRole(role);
    }

    @Test
    @DisplayName("Debe lanzar InvalidTokenException si el refresh token no existe en Redis")
    void refreshToken_debeLanzarExcepcion_siTokenNoExisteEnRedis() {
        // Arrange: Simula que Redis no encuentra la clave del token (retorna null)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("RT:refresh-token-valido-123")).thenReturn(null);

        // Act & Assert: Verifica que la falta de token en caché dispare la excepción de token inválido
        assertThrows(InvalidTokenException.class, () -> refreshTokenService.refreshToken(tokenRequest));

        // Verify: Confirma que la ejecución se detuvo y no consultó repositorios ni servicios
        verify(valueOperations).get("RT:refresh-token-valido-123");
        verifyNoInteractions(userRepository, userDetailsService, jwtService);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el usuario asociado al token no existe")
    void refreshToken_debeLanzarExcepcion_siUsuarioNoExiste() {
        // Arrange: Redis encuentra el email, pero el usuario no existe en la base de datos
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("RT:refresh-token-valido-123")).thenReturn("ana@usi.com");
        when(userRepository.findByEmail("ana@usi.com")).thenReturn(Optional.empty());

        // Act & Assert: Verifica que se lance la excepción cuando el usuario fue eliminado
        assertThrows(ResourceNotFoundException.class, () -> refreshTokenService.refreshToken(tokenRequest));

        // Verify: Valida la consulta al repositorio y asegura que no se generó ningún JWT
        verify(userRepository).findByEmail("ana@usi.com");
        verifyNoInteractions(userDetailsService, jwtService);
    }

    @Test
    @DisplayName("Debe renovar el Access Token y refrescar la expiración en Redis si el token es válido")
    void refreshToken_debeRenovarAccessTokenYActualizarExpiracion_siTokenEsValido() {
        // Arrange: Simula la presencia del token en Redis, usuario existente y generación exitosa del JWT
        UserDetails userDetailsMock = mock(UserDetails.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("RT:refresh-token-valido-123")).thenReturn("ana@usi.com");
        when(userRepository.findByEmail("ana@usi.com")).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("ana@usi.com")).thenReturn(userDetailsMock);
        when(jwtService.generateAccessToken(userDetailsMock)).thenReturn("NUEVO_ACCESS_TOKEN");

        // Act: Ejecución del refresco de token
        AuthResponseDto response = refreshTokenService.refreshToken(tokenRequest);

        // Assert: Agrupa comprobaciones sobre el DTO resultante
        assertAll(
                () -> assertEquals("NUEVO_ACCESS_TOKEN", response.accessToken()),
                () -> assertEquals("refresh-token-valido-123", response.refreshToken()),
                () -> assertEquals(userId, response.userId()),
                () -> assertEquals("Ana López", response.fullName()),
                () -> assertEquals("USER", response.roleName())
        );

        // Verify: Revisa que se extendió el tiempo de expiración de la clave en Redis
        verify(redisTemplate).expire(eq("RT:refresh-token-valido-123"), any(Duration.class));
    }
}
