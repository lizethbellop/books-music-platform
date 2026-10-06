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

        //extraer el token (quitando los primeros 7 caracteres de "Bearer ")
        jwtToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(jwtToken);

        //Si el token tiene correo y el usuario aún no está autenticado en el contexto actual
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            //Verificar firma y validez temporal del token
            if (jwtService.isTokenValid(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);//registrar al usuario en el contexto de seguridad para la petición actual
            }
        }

        //permitir que la petición continúe su flujo
        filterChain.doFilter(request, response);
    }
}
