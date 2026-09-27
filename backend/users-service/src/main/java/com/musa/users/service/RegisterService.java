package com.musa.users.service;

import com.musa.users.dto.request.RegisterRequestDto;
import com.musa.users.dto.response.MessageResponseDto;

public interface RegisterService {
    MessageResponseDto register(RegisterRequestDto request);
}