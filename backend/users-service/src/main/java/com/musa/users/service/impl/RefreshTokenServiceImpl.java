package com.musa.users.service.impl;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.entity.User;
import com.musa.users.exception.InvalidTokenException;
import com.musa.users.exception.ResourceNotFoundException;
import com.musa.users.repository.UserRepository;
import com.musa.users.service.JwtService;
import com.musa.users.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Implementación del servicio para la renovación de tokens de acceso e invalidación/rotación de sesiones mediante Redis.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    @Override
    public AuthResponseDto refreshToken(TokenRequestDto request) {
        String refreshToken = request.refreshToken();
        String redisRefreshKey = "RT:" + refreshToken;

        String email = redisTemplate.opsForValue().get(redisRefreshKey);

        if (email == null) {
            throw new InvalidTokenException("El token de refresco es inválido o ha expirado.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el email: " + email));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        redisTemplate.expire(redisRefreshKey, Duration.ofMillis(refreshTokenExpiration));

        return new AuthResponseDto(
                newAccessToken,
                refreshToken,
                user.getId(),
                user.getFullName(),
                user.getRole().getName()
        );
    }
}
