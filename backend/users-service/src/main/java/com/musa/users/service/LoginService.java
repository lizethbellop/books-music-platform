package com.musa.users.service;

import com.musa.users.dto.request.LoginRequestDto;
import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.exception.AccountDisabledException;
import com.musa.users.exception.InvalidCredentialsException;

/**
 * Interfaz de servicio para la autenticación e inicio de sesión de usuarios.
 */
public interface LoginService {

    /**
     * Autentica las credenciales de un usuario y retorna los tokens de acceso y sesión.
     *
     * @param request DTO con el correo electrónico y la contraseña del usuario.
     * @return {@link AuthResponseDto} Datos de autenticación y tokens JWT/Sesión.
     * @throws InvalidCredentialsException Si el correo o la contraseña son incorrectos.
     * @throws AccountDisabledException   Si la cuenta del usuario se encuentra inactiva o deshabilitada.
     */
    AuthResponseDto login(LoginRequestDto request);
}