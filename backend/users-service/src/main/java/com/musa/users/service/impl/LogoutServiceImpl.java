package com.musa.users.service.impl;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.LogoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de cierre de sesión mediante la eliminación e invalidación de tokens de refresco en Redis.
 */
@Service
@RequiredArgsConstructor
public class LogoutServiceImpl implements LogoutService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public MessageResponseDto logout(TokenRequestDto request) {
        String redisRefreshKey = "RT:" + request.refreshToken();

        redisTemplate.delete(redisRefreshKey);

        return new MessageResponseDto("Sesión cerrada exitosamente.");
    }
}
