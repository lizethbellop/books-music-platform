package com.musa.users.service.impl;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.entity.User;
import com.musa.users.exception.AccountDisabledException;
import com.musa.users.exception.InvalidTokenException;
import com.musa.users.repository.UserRepository;
import com.musa.users.service.JwtService;
import com.musa.users.service.RefreshTokenService;
import com.musa.users.session.RefreshSession;
import com.musa.users.session.RefreshSessionStore;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshSessionStore refreshSessionStore;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    public AuthResponseDto refreshToken(TokenRequestDto request) {
        RefreshSession session = refreshSessionStore
                .find(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException(
                        "El token de refresco es inválido o ha expirado."
                ));

        User user = userRepository.findByEmailWithRole(session.email())
                .orElseThrow(() -> new InvalidTokenException(
                        "La sesión ya no es válida."
                ));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            refreshSessionStore.revoke(request.refreshToken());

            throw new AccountDisabledException(
                    "La cuenta se encuentra desactivada."
            );
        }

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(session.email());

        String accessToken = jwtService.generateAccessToken(
                userDetails,
                user.getId(),
                session.sessionExpiresAt()
        );

        String refreshToken = refreshSessionStore
                .rotate(request.refreshToken(), session)
                .orElseThrow(() -> new InvalidTokenException(
                        "El token de refresco ya no es válido."
                ));

        return new AuthResponseDto(
                accessToken,
                refreshToken,
                user.getId(),
                user.getFullName(),
                user.getRole().getName(),
                jwtService.getAccessTokenExpiresAt(accessToken),
                session.sessionExpiresAt(),
                user.getUsername()
        );
    }
}