package com.musa.users.service;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.MessageResponseDto;

public interface LogoutService {
    MessageResponseDto logout(TokenRequestDto request);
}