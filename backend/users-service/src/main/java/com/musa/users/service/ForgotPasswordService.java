package com.musa.users.service;

import com.musa.users.dto.request.ForgotPasswordRequestDto;
import com.musa.users.dto.response.MessageResponseDto;

public interface ForgotPasswordService {
    MessageResponseDto forgotPassword(ForgotPasswordRequestDto request);
}