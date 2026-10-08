package com.musa.users.service.impl;

import com.musa.users.dto.request.TokenRequestDto;
import com.musa.users.dto.response.MessageResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.musa.users.session.RefreshSessionStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutServiceImplTest {

    @Mock
    private RefreshSessionStore refreshSessionStore;

    @InjectMocks
    private LogoutServiceImpl logoutService;

    @Test
    @DisplayName("Debe eliminar el Refresh Token de Redis y retornar mensaje de cierre de sesión exitoso")
    void logout_debeEliminarRefreshTokenDeRedisYDevolverMensajeExitoso() {
        // Arrange
        String refreshToken = "uuid-refresh-token-12345";
        TokenRequestDto request = new TokenRequestDto(refreshToken);

        // Act
        MessageResponseDto response = logoutService.logout(request);

        // Assert
        assertEquals("Sesión cerrada exitosamente.", response.message());

        // Verify: revoca la sesión mediante el almacén de refresh tokens
        verify(refreshSessionStore).revoke(refreshToken);
    }
}