package com.musa.users.controller;

import com.musa.users.dto.request.RegisterRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import com.musa.users.service.RegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Controlador REST para el registro de nuevos usuarios en el sistema. */
@Tag(name = "Autenticación", description = "Endpoints para la gestión de autenticación, acceso y recuperación de cuentas")

@RestController
@RequestMapping("/api/v1/auth")
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @Operation(summary = "Registrar un nuevo usuario", description = "Crea una nueva cuenta de usuario asignando el rol especificado y validando la complejidad de la contraseña.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Error de validación en los datos o contraseñas no coinciden",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "El correo electrónico ya se encuentra registrado",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class)))
    })

    @PostMapping("/register")
    public ResponseEntity<MessageResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {

        MessageResponseDto response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}