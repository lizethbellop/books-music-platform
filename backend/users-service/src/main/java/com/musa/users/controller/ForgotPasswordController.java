package com.musa.users.controller;

import com.musa.users.dto.request.ForgotPasswordRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.ForgotPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador REST para la solicitud de recuperación de contraseña. */
@Tag(name = "Autenticación", description = "Endpoints para la gestión de autenticación, acceso y recuperación de cuentas")

@RestController
@RequestMapping("/api/v1/auth")
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    public ForgotPasswordController(ForgotPasswordService forgotPasswordService) {
        this.forgotPasswordService = forgotPasswordService;
    }

    @Operation(summary = "Solicitar recuperación de contraseña", description = "Genera un token de recuperación y envía un correo electrónico al usuario para restablecer su contraseña.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Correo de recuperación enviado exitosamente",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o formato de correo incorrecto",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Error técnico al enviar el correo electrónico",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDto> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        MessageResponseDto response = forgotPasswordService.sendResetPasswordEmail(request);
        return ResponseEntity.ok(response);
    }
}



