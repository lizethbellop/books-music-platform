package com.musa.users.service.impl;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.LogoutService;
import com.musa.users.session.RefreshSessionStore;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutServiceImpl implements LogoutService {

    private final RefreshSessionStore refreshSessionStore;

    @Override
    public MessageResponseDto logout(TokenRequestDto request) {
        refreshSessionStore.revoke(request.refreshToken());

        return new MessageResponseDto(
                "Sesión cerrada exitosamente."
        );
    }
}