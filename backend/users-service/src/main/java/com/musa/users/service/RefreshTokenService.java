package com.musa.users.service;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.exception.InvalidTokenException;
import com.musa.users.exception.TokenExpiredException;

/**
 * Interfaz de servicio para la renovación de tokens de acceso expirados mediante tokens de refresco.
 */
public interface RefreshTokenService {

    /**
     * Valida el token de refresco y genera un nuevo token de acceso JWT.
     *
     * @param request DTO con el token de refresco activo.
     * @return {@link AuthResponseDto} Nuevo token de acceso e información del usuario.
     * @throws InvalidTokenException Si el token de refresco es inválido o no existe.
     * @throws TokenExpiredException Si el token de refresco ha superado su vigencia.
     */
    AuthResponseDto refreshToken(TokenRequestDto request);
}