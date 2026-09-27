package com.musa.users.service;

import com.musa.users.dto.request.LoginRequestDto;
import com.musa.users.dto.response.AuthResponseDto;

public interface LoginService {
    AuthResponseDto login(LoginRequestDto request);
}