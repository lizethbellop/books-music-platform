package com.musa.users.service.impl;

import com.musa.users.dto.request.ResetPasswordRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.entity.PasswordReset;
import com.musa.users.entity.User;
import com.musa.users.exception.InvalidTokenException;
import com.musa.users.exception.SamePasswordException;
import com.musa.users.exception.TokenExpiredException;
import com.musa.users.repository.PasswordResetRepository;
import com.musa.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.musa.users.exception.PasswordMismatchException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ResetPasswordServiceImpl resetPasswordService;

    private ResetPasswordRequestDto requestDto;
    private PasswordReset resetEntity;
    private User user;

    @BeforeEach
    void setUp() {
        // Se proporcionan los 3 campos obligatorios del DTO: token, newPassword y confirmNewPassword
        requestDto = new ResetPasswordRequestDto("token-valido-xyz", "NuevaPassword123!", "NuevaPassword123!");

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("ana@usi.com");
        user.setPasswordHash("HASH_ACTUAL");

        resetEntity = new PasswordReset();
        resetEntity.setId(UUID.randomUUID());
        resetEntity.setTokenHash("token-valido-xyz");
        resetEntity.setUser(user);
        resetEntity.setExpiresAt(LocalDateTime.now().plusHours(1)); // Token vigente
    }

    @Test
    @DisplayName("Debe lanzar InvalidTokenException si el token de recuperación no existe")
    void resetPassword_debeLanzarExcepcion_siTokenNoExiste() {
        // Arrange: Simula token inexistente en la base de datos
        when(passwordResetRepository.findByTokenHash("token-valido-xyz")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> resetPasswordService.resetPassword(requestDto));

        // Verify: Asegura que no interactúa con encriptadores ni repositorios de usuarios
        verify(passwordResetRepository).findByTokenHash("token-valido-xyz");
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Debe eliminar el token y lanzar TokenExpiredException si el token ya expiró")
    void resetPassword_debeEliminarTokenYLanzarExcepcion_siTokenHaExpirado() {
        // Arrange: Configura una fecha de expiración en el pasado
        resetEntity.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        when(passwordResetRepository.findByTokenHash("token-valido-xyz")).thenReturn(Optional.of(resetEntity));

        // Act & Assert
        assertThrows(TokenExpiredException.class, () -> resetPasswordService.resetPassword(requestDto));

        // Verify: Verifica la eliminación del token caducado en la base de datos
        verify(passwordResetRepository).delete(resetEntity);
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Debe lanzar SamePasswordException si la nueva contraseña es idéntica a la actual")
    void resetPassword_debeLanzarExcepcion_siNuevaPasswordEsIgualAActual() {
        // Arrange: Simula que la comparación de contraseñas devuelve verdadero
        when(passwordResetRepository.findByTokenHash("token-valido-xyz")).thenReturn(Optional.of(resetEntity));
        when(passwordEncoder.matches("NuevaPassword123!", "HASH_ACTUAL")).thenReturn(true);

        // Act & Assert
        assertThrows(SamePasswordException.class, () -> resetPasswordService.resetPassword(requestDto));

        // Verify: Confirma que no actualiza al usuario ni elimina el token si la validación falla
        verify(passwordEncoder).matches("NuevaPassword123!", "HASH_ACTUAL");
        verify(userRepository, never()).save(any());
        verify(passwordResetRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe actualizar la contraseña, guardar el usuario y eliminar el token usado")
    void resetPassword_debeActualizarPasswordYEliminarToken_siDatosSonValidos() {
        // Arrange: Flujo de ejecución exitoso
        when(passwordResetRepository.findByTokenHash("token-valido-xyz")).thenReturn(Optional.of(resetEntity));
        when(passwordEncoder.matches("NuevaPassword123!", "HASH_ACTUAL")).thenReturn(false);
        when(passwordEncoder.encode("NuevaPassword123!")).thenReturn("NUEVO_HASH_ENCRIPTADO");

        // Act
        MessageResponseDto response = resetPasswordService.resetPassword(requestDto);

        // Assert
        assertEquals("Contraseña restablecida exitosamente.", response.message());
        assertEquals("NUEVO_HASH_ENCRIPTADO", user.getPasswordHash());

        // Verify: Confirma la persistencia del usuario actualizado y el borrado del token consumido
        verify(userRepository).save(user);
        verify(passwordResetRepository).delete(resetEntity);
    }

    @Test
    @DisplayName("Debe rechazar contraseñas que no coinciden sin modificar datos")
    void resetPassword_debeLanzarExcepcion_siPasswordsNoCoinciden() {

        ResetPasswordRequestDto request = new ResetPasswordRequestDto(
                "token-valido-xyz",
                "NuevaPassword123!",
                "OtraPassword123!"
        );


        assertThrows(
                PasswordMismatchException.class,
                () -> resetPasswordService.resetPassword(request)
        );


        verifyNoInteractions(
                passwordResetRepository,
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void verifyToken_acceptsValidCodeWithoutConsumingIt() {
        when(passwordResetRepository.findByTokenHash("code")).thenReturn(Optional.of(resetEntity));
        assertEquals("Token válido.", resetPasswordService.verifyToken(
                new com.musa.users.dto.request.VerifyTokenRequestDto("code")).message());
        verify(passwordResetRepository, never()).delete(any());
    }

    @Test
    void verifyToken_rejectsUnknownCode() {
        when(passwordResetRepository.findByTokenHash("missing")).thenReturn(Optional.empty());
        assertThrows(InvalidTokenException.class, () -> resetPasswordService.verifyToken(
                new com.musa.users.dto.request.VerifyTokenRequestDto("missing")));
    }

    @Test
    void verifyToken_rejectsExpiredCode() {
        resetEntity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(passwordResetRepository.findByTokenHash("code")).thenReturn(Optional.of(resetEntity));
        assertThrows(TokenExpiredException.class, () -> resetPasswordService.verifyToken(
                new com.musa.users.dto.request.VerifyTokenRequestDto("code")));
    }
}