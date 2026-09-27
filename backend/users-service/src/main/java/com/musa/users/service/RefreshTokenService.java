package com.musa.users.service;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.AuthResponseDto;

public interface RefreshTokenService {
    AuthResponseDto refreshToken(TokenRequestDto request);
}