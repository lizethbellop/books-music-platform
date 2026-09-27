package com.musa.users.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String token);
    void sendWelcomeEmail(String toEmail, String fullName);
}