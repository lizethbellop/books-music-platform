package com.musa.users.service;
import com.musa.users.exception.EmailSendException;
/**
 * Interfaz de servicio para la generación y envío de notificaciones por correo electrónico del sistema.
 */
public interface EmailService {

    /**
     * Envía un correo electrónico con el token necesario para restablecer la contraseña del usuario.
     *
     * @param toEmail Dirección de correo electrónico del destinatario.
     * @param token   Token único de recuperación generado para el usuario.
     * @throws EmailSendException Si ocurre un error de comunicación con el servidor SMTP.
     */
    void sendPasswordResetEmail(String toEmail, String token);

    /**
     * Envía un correo electrónico de bienvenida tras un registro exitoso en la plataforma.
     *
     * @param toEmail  Dirección de correo electrónico del destinatario.
     * @param fullName Nombre completo del usuario registrado.
     */
    void sendWelcomeEmail(String toEmail, String fullName);
}