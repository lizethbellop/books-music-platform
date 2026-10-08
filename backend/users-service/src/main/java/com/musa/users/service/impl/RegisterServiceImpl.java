package com.musa.users.service.impl;

import com.musa.users.dto.request.RegisterRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.entity.Role;
import com.musa.users.entity.User;
import com.musa.users.exception.PasswordMismatchException;
import com.musa.users.exception.ResourceNotFoundException;
import com.musa.users.exception.UserAlreadyExistsException;
import com.musa.users.repository.RoleRepository;
import com.musa.users.repository.UserRepository;
import com.musa.users.service.EmailService;
import com.musa.users.service.RegisterService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Locale;
import java.sql.SQLException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio para la creación y registro de nuevos usuarios en PostgreSQL con notificación de bienvenida.
 */
@Service
public class RegisterServiceImpl implements RegisterService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public RegisterServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public MessageResponseDto register(RegisterRequestDto request) {

        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordMismatchException("Las contraseñas no coinciden.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("El correo " + request.email() + " ya se encuentra registrado.");
        }

        String username = request.username().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UserAlreadyExistsException("El nombre de usuario ya está registrado.");
        }

        Role role = roleRepository.findByName(request.roleName())
                .orElseThrow(() -> new ResourceNotFoundException("El rol '" + request.roleName() + "' no existe."));

        User user = new User();
        user.setFullName(request.fullName());
        user.setUsername(username);
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setIsActive(true);

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // La restricción única también protege registros simultáneos.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                    throw new UserAlreadyExistsException("El correo o nombre de usuario ya está registrado.");
                }
            }
            throw exception;
        }
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName());
        return new MessageResponseDto("Registro exitoso.");

    }
}