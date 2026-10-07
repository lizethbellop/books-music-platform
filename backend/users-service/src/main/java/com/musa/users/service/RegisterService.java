package com.musa.users.service;

import com.musa.users.dto.request.RegisterRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.exception.PasswordMismatchException;
import com.musa.users.exception.ResourceNotFoundException;
import com.musa.users.exception.UserAlreadyExistsException;
import com.musa.users.exception.WeakPasswordException;

/**
 * Interfaz de servicio para el registro de nuevos usuarios en la plataforma.
 */
public interface RegisterService {

    /**
     * Registra un nuevo usuario en el sistema previa validación de duplicidad, rol y contraseñas.
     *
     * @param request DTO con los datos del registro (nombre, correo, contraseña, rol).
     * @return {@link MessageResponseDto} Mensaje de confirmación del registro exitoso.
     * @throws UserAlreadyExistsException Si el correo electrónico ya se encuentra registrado.
     * @throws PasswordMismatchException  Si las contraseñas ingresadas no coinciden.
     * @throws WeakPasswordException      Si la contraseña no cumple con las reglas de seguridad.
     * @throws ResourceNotFoundException  Si el rol especificado no existe en el sistema.
     */
    MessageResponseDto register(RegisterRequestDto request);
}