package com.musa.users.service.impl;

import com.musa.users.dto.request.ForgotPasswordRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.entity.PasswordReset;
import com.musa.users.entity.User;
import com.musa.users.exception.EmailSendException;
import com.musa.users.repository.PasswordResetRepository;
import com.musa.users.repository.UserRepository;
import com.musa.users.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ForgotPasswordServiceImpl forgotPasswordService;

    private ForgotPasswordRequestDto requestDto;
    private User user;

    @BeforeEach
    void setUp() {
        requestDto = new ForgotPasswordRequestDto("ana@usi.com");

        user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Ana López");
        user.setEmail("ana@usi.com");
    }

    @Test
    @DisplayName("Debe devolver un mensaje genérico si el usuario no existe")
    void sendResetPasswordEmail_debeDevolverMensajeGenerico_siUsuarioNoExiste() {

        when(userRepository.findByEmail("ana@usi.com"))
                .thenReturn(Optional.empty());


        MessageResponseDto response =
                forgotPasswordService.sendResetPasswordEmail(requestDto);


        assertEquals(
                "Si existe una cuenta con ese correo, recibirás instrucciones "
                        + "para restablecer tu contraseña.",
                response.message()
        );


        verify(userRepository).findByEmail("ana@usi.com");
        verifyNoInteractions(passwordResetRepository, emailService);
    }

    @Test
    @DisplayName("Debe lanzar EmailSendException si ocurre un fallo al enviar el correo")
    void sendResetPasswordEmail_debeLanzarExcepcion_siFallaEnvioDeCorreo() {
        // Arrange: Prepara la búsqueda del usuario y fuerza una falla en el servicio de correo
        when(userRepository.findByEmail("ana@usi.com")).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("Fallo del servidor SMTP"))
                .when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // Act & Assert: Captura la conversión de la excepción a EmailSendException
        assertThrows(EmailSendException.class, () -> forgotPasswordService.sendResetPasswordEmail(requestDto));

        // Verify: Confirma que la entidad PasswordReset sí se intentó persistir antes de la falla
        verify(passwordResetRepository).save(any(PasswordReset.class));
        verify(emailService).sendPasswordResetEmail(eq("ana@usi.com"), anyString());
    }

    @Test
    @DisplayName("Debe guardar el token de restablecimiento y enviar el correo cuando el usuario existe")
    void sendResetPasswordEmail_debeGuardarTokenYEnviarCorreo_siUsuarioExiste() {
        // Arrange
        when(userRepository.findByEmail("ana@usi.com")).thenReturn(Optional.of(user));

        // Act
        MessageResponseDto response = forgotPasswordService.sendResetPasswordEmail(requestDto);

        // Assert: Revisa la respuesta exitosa y captura la entidad guardada
        assertEquals(
                "Si existe una cuenta con ese correo, recibirás instrucciones "
                        + "para restablecer tu contraseña.",
                response.message()
        );

        ArgumentCaptor<PasswordReset> resetCaptor = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository).save(resetCaptor.capture());

        PasswordReset savedToken = resetCaptor.getValue();

        assertAll(
                () -> assertNotNull(savedToken.getTokenHash()),
                () -> assertEquals(user, savedToken.getUser())
        );

        // Verify: Confirma que se disparó la transmisión del email con el token generado
        verify(emailService).sendPasswordResetEmail(eq("ana@usi.com"), eq(savedToken.getTokenHash()));
    }
}
