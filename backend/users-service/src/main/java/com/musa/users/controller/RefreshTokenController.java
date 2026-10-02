package com.musa.users.controller;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.AuthResponseDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.RefreshTokenService;
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

/** Controlador REST para la renovación de tokens de acceso JWT. */
@Tag(name = "Autenticación", description = "Endpoints para la gestión de autenticación, acceso y recuperación de cuentas")

@RestController
@RequestMapping("/api/v1/auth")
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Operation(summary = "Renovar token de acceso", description = "Genera un nuevo token de acceso a partir de un token de refresco válido y no expirado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token renovado exitosamente",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Token de refresco no proporcionado",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token de refresco inválido o expirado",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponseDto> refreshToken(@Valid @RequestBody TokenRequestDto request) {
        AuthResponseDto response = refreshTokenService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
