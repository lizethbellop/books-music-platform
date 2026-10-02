package com.musa.users.service;

import com.musa.users.dto.request.ForgotPasswordRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.exception.EmailSendException;
import com.musa.users.exception.ResourceNotFoundException;

/**
 * Interfaz de servicio para la gestión de solicitudes de recuperación de contraseña.
 */
public interface ForgotPasswordService {

    /**
     * Procesa la solicitud de recuperación de contraseña, generando un token único y enviándolo por correo.
     *
     * @param request DTO que contiene el correo electrónico del usuario que solicita el restablecimiento.
     * @return {@link MessageResponseDto} Mensaje de confirmación sobre el envío del correo.
     * @throws ResourceNotFoundException Si el correo electrónico no pertenece a ningún usuario registrado.
     * @throws EmailSendException        Si ocurre un fallo durante el envío del correo electrónico.
     */
    MessageResponseDto sendResetPasswordEmail(ForgotPasswordRequestDto request);
}