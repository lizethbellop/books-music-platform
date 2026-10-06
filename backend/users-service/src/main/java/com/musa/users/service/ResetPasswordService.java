package com.musa.users.service;

import com.musa.users.dto.request.ResetPasswordRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.exception.InvalidTokenException;
import com.musa.users.exception.PasswordMismatchException;
import com.musa.users.exception.SamePasswordException;
import com.musa.users.exception.TokenExpiredException;
import com.musa.users.exception.WeakPasswordException;

/**
 * Interfaz de servicio para la actualización de contraseña mediante tokens de recuperación.
 */
public interface ResetPasswordService {
    /**
     * Actualiza la contraseña del usuario validando el token de recuperación y la seguridad de la clave.
     *
     * @param request DTO con el token de recuperación, la nueva contraseña y su confirmación.
     * @return {@link MessageResponseDto} Mensaje de confirmación sobre el cambio de contraseña.
     * @throws InvalidTokenException     Si el token de recuperación no existe o ya fue utilizado.
     * @throws TokenExpiredException     Si el token de recuperación ha superado su límite de tiempo.
     * @throws PasswordMismatchException Si la nueva contraseña y su confirmación no coinciden.
     * @throws WeakPasswordException     Si la nueva contraseña no cumple los requisitos mínimos.
     * @throws SamePasswordException     Si la nueva contraseña es igual a la actual.
     */
    MessageResponseDto resetPassword(ResetPasswordRequestDto request);
}
