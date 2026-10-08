package com.musa.users.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.function.Function;
import java.time.Instant;
import java.util.UUID;

/**
 * Interfaz de servicio para la generación, extracción de claims y validación de tokens JWT.
 */
public interface JwtService {

    /**
     * Genera un token de acceso JWT estándar a partir de los detalles del usuario.
     *
     * @param userDetails Detalles del usuario autenticado.
     * @return String representación compacta del token JWT generado.
     */
    String generateAccessToken(UserDetails userDetails);

    /**
     * Genera un token de acceso JWT incluyendo claims o información adicional.
     *
     * @param extraClaims Mapa con propiedades adicionales que se incluirán en el payload del token.
     * @param userDetails Detalles del usuario autenticado.
     * @return String representación compacta del token JWT generado.
     */
    String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails);

    /**
     * Extrae el nombre de usuario (subject) contenido dentro del token JWT.
     *
     * @param token Cadena con el token JWT a inspeccionar.
     * @return String el nombre de usuario o correo asociado al token.
     */
    String extractUsername(String token);

    /**
     * Extrae un claim específico del token JWT utilizando una función resolutora.
     *
     * @param <T>            Tipo de dato esperado del claim.
     * @param token          Cadena con el token JWT.
     * @param claimsResolver Función para resolver y extraer el claim deseado.
     * @return T el valor del claim extraído.
     */
    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    /**
     * Valida si un token JWT pertenece al usuario especificado y si aún no ha expirado.
     *
     * @param token       Cadena con el token JWT.
     * @param userDetails Detalles del usuario contra el cual se valida el token.
     * @return boolean true si el token es válido y vigente, false en caso contrario.
     */
    boolean isTokenValid(String token, UserDetails userDetails);

    String generateAccessToken(
            UserDetails userDetails,
            Instant sessionExpiresAt
    );

    Instant getAccessTokenExpiresAt(String token);

    String generateAccessToken(
            UserDetails userDetails,
            UUID userId,
            Instant sessionExpiresAt
    );

}
