package com.musa.users.service.impl;

import com.musa.users.dto.request.LoginRequestDto;
import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.entity.User;
import com.musa.users.exception.AccountDisabledException;
import com.musa.users.exception.InvalidCredentialsException;
import com.musa.users.repository.UserRepository;
import com.musa.users.service.JwtService;
import com.musa.users.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import com.musa.users.session.RefreshSession;
import com.musa.users.session.RefreshSessionStore;
import java.time.Instant;

/**
 * Implementación del servicio de inicio de sesión y autenticación con control de bloqueos por intentos fallidos en Redis.
 */
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;
    private final RefreshSessionStore refreshSessionStore;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    @Value("${application.security.lock.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${application.security.lock.time-minutes}")
    private long lockTimeMinutes;

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        String email = request.email();
        String lockKey = "lock:" + email;
        String attemptsKey = "attempts:" + email;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new InvalidCredentialsException("Cuenta bloqueada temporalmente por intentos fallidos. Intente más tarde.");
        }

        User user = userRepository.findByEmailWithRole(email)
                .orElseThrow(() ->
                        new InvalidCredentialsException("Credenciales inválidas."));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountDisabledException("La cuenta se encuentra desactivada.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(attemptsKey, lockKey);
            throw new InvalidCredentialsException("Credenciales inválidas.");
        }

        redisTemplate.delete(attemptsKey);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        Instant sessionExpiresAt = Instant.ofEpochMilli(System.currentTimeMillis())
                .plusMillis(refreshTokenExpiration);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        String accessToken = jwtService.generateAccessToken(
                userDetails,
                user.getId(),
                sessionExpiresAt
        );

        String refreshToken = refreshSessionStore.create(
                new RefreshSession(
                        email,
                        sessionExpiresAt,
                        request.rememberMe()
                )
        );

        return new AuthResponseDto(
                accessToken,
                refreshToken,
                user.getId(),
                user.getFullName(),
                user.getRole().getName(),
                jwtService.getAccessTokenExpiresAt(accessToken),
                sessionExpiresAt,
                user.getUsername()
        );
    }

    private void registerFailedAttempt(String attemptsKey, String lockKey) {
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(lockTimeMinutes));
        }

        if (attempts != null && attempts >= maxFailedAttempts) {
            redisTemplate.opsForValue().set(lockKey, "LOCKED", Duration.ofMinutes(lockTimeMinutes));
            redisTemplate.delete(attemptsKey);
        }
    }
}