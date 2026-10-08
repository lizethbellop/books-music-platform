package com.musa.users.security;

import com.musa.users.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.AuthenticationException;

/**
 * Filtro de seguridad que intercepta cada petición HTTP entrante para validar
 * el token de autenticación JWT presente en la cabecera 'Authorization'.
 * <p>
 * Extiende {@link OncePerRequestFilter} para garantizar una única ejecución por cada petición HTTP.
 * Si el token es válido, registra el objeto de autenticación en el {@link SecurityContextHolder}.
 * </p>
 * */

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwtToken;
        final String userEmail;

        // si no viene el header 'Authorization' o no empieza con 'Bearer ', se omite el filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtToken = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwtToken);

            if (userEmail == null) {
                rejectToken(response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()
                        || !userDetails.isAccountNonExpired() || !userDetails.isCredentialsNonExpired()
                        || !jwtService.isTokenValid(jwtToken, userDetails)) {
                    rejectToken(response);
                    return;
                }
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException | AuthenticationException | IllegalArgumentException exception) {
            rejectToken(response);
            return;
        }

        //permitir que la petición continúe su flujo
        filterChain.doFilter(request, response);
    }
    private void rejectToken(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"Token inválido o expirado. Inicia sesión nuevamente.\"}");
    }
}
