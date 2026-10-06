package com.musa.users.service.impl;

import com.musa.users.dto.request.ForgotPasswordRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.entity.PasswordReset; // <--- Tu entidad REAL
import com.musa.users.entity.User;
import com.musa.users.exception.EmailSendException;
import com.musa.users.exception.ResourceNotFoundException;
import com.musa.users.repository.PasswordResetRepository;
import com.musa.users.repository.UserRepository;
import com.musa.users.service.EmailService;
import com.musa.users.service.ForgotPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementación del servicio para el procesamiento de solicitudes de recuperación de contraseña.
 */
@Service
@RequiredArgsConstructor
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final EmailService emailService;

    @Override
    public MessageResponseDto sendResetPasswordEmail(ForgotPasswordRequestDto request) {
        //request.email() porque ForgotPasswordRequestDto es un record
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con el correo: " + request.email()));

        //generar el token único
        String resetToken = UUID.randomUUID().toString();

        //crear instancia de la entidad PasswordReset (NO Repository)
        PasswordReset tokenEntity = new PasswordReset();
        tokenEntity.setTokenHash(resetToken);
        tokenEntity.setUser(user);

        // Guardar la entidad en PostgreSQL
        passwordResetRepository.save(tokenEntity);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        } catch (Exception ex) {
            throw new EmailSendException("Error al enviar el correo de recuperación.");
        }

        return new MessageResponseDto("Correo de recuperación enviado exitosamente.");
    }
}




