package com.musa.users.service;

import com.musa.users.dto.request.ResetPasswordRequestDto;
import com.musa.users.dto.response.MessageResponseDto;

public interface ResetPasswordService {
    MessageResponseDto resetPassword(ResetPasswordRequestDto request);
}
