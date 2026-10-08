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
import com.musa.users.service.ResetPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.musa.users.exception.PasswordMismatchException;

/**
 * Implementación del servicio para el restablecimiento y actualización de contraseñas mediante tokens temporales.
 */
@Service
@RequiredArgsConstructor
public class ResetPasswordServiceImpl implements ResetPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public MessageResponseDto resetPassword(ResetPasswordRequestDto request) {

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new PasswordMismatchException(
                    "Las contraseñas no coinciden."
            );
        }

        PasswordReset resetEntity = passwordResetRepository.findByTokenHash(request.token())
                .orElseThrow(() -> new InvalidTokenException("El token de recuperación es inválido."));

        if (resetEntity.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            passwordResetRepository.delete(resetEntity);
            throw new TokenExpiredException("El token de recuperación ha expirado.");
        }

        User user = resetEntity.getUser();

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new SamePasswordException("La nueva contraseña no puede ser igual a la contraseña actual.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        passwordResetRepository.delete(resetEntity);

        return new MessageResponseDto("Contraseña restablecida exitosamente.");
    }
}