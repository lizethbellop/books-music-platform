package com.musa.users.service;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.exception.InvalidTokenException;

/**
 * Interfaz de servicio para el cierre de sesión e invalidación de tokens de actualización.
 */
public interface LogoutService {

    /**
     * Invalida la sesión activa del usuario eliminando o revocando su token de refresco.
     *
     * @param request DTO con el token de refresco que se desea invalidar.
     * @return {@link MessageResponseDto} Mensaje de confirmación del cierre de sesión.
     * @throws InvalidTokenException Si el token proporcionado no existe o es inválido.
     */
    MessageResponseDto logout(TokenRequestDto request);
}