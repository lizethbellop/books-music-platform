package com.musa.users.controller;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.LogoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Controlador REST para la invalidación de sesiones (cierre de sesión). */
@Tag(name = "Autenticación", description = "Endpoints para la gestión de autenticación, acceso y recuperación de cuentas")

@RestController
@CrossOrigin(origins = "*") // <--- Permite peticiones desde cualquier origen (Flutter Web)
@RequestMapping("/api/v1/auth")
public class LogoutController {

    private final LogoutService logoutService;

    public LogoutController(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    @Operation(summary = "Cerrar sesión", description = "Invalida el token de refresco del usuario eliminando la sesión activa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión cerrada exitosamente",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Token de refresco no proporcionado o con formato inválido",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token de refresco inválido o no existente",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })

    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDto> logout(@Valid @RequestBody TokenRequestDto request) {
        MessageResponseDto response = logoutService.logout(request);
        return ResponseEntity.ok(response);
    }
}
