package com.musa.users.controller;

import com.musa.users.dto.request.ResetPasswordRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.ResetPasswordService;
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

/** Controlador REST para el restablecimiento de contraseñas mediante token. */
@Tag(name = "Autenticación", description = "Endpoints para la gestión de autenticación, acceso y recuperación de cuentas")

@RestController
@RequestMapping("/api/v1/auth")
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    public ResetPasswordController(ResetPasswordService resetPasswordService) {
        this.resetPasswordService = resetPasswordService;
    }

    @Operation(summary = "Restablecer contraseña", description = "Valida el token de recuperación y actualiza la contraseña del usuario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Contraseñas no coinciden, contraseña débil o idéntica a la anterior",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token de recuperación inválido, alterado o expirado",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDto> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        MessageResponseDto response = resetPasswordService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}