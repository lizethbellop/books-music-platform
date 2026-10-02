package com.musa.users.service.impl;

import com.musa.users.exception.EmailSendException;
import com.musa.users.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementación del servicio de envío de correos electrónicos mediante Spring JavaMailSender.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@musa.com}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Restablecimiento de Contraseña - MUSA");
            message.setText("Hola,\n\nHas solicitado restablecer tu contraseña. "
                    + "Utiliza el siguiente token de recuperación (válido por 5 minutos):\n\n"
                    + token + "\n\nSi no solicitaste este cambio, ignora este mensaje.");

            mailSender.send(message);
        } catch (Exception e) {
            throw new EmailSendException("No fue posible enviar el correo de recuperación a " + toEmail);
        }
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("¡Bienvenido a MUSA!");
            message.setText("¡Hola " + fullName + "!\n\nTu cuenta ha sido creada exitosamente. "
                    + "Ya puedes iniciar sesión en la plataforma.");

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo enviar el correo de bienvenida: " + e.getMessage());
        }
    }
}