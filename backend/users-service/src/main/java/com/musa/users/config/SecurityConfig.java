package com.musa.users.config;

import com.musa.users.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de la seguridad web del sistema.
 * <p>
 * Esta clase define la cadena de filtros de seguridad ({@link SecurityFilterChain}),
 * deshabilita CSRF para arquitectura REST stateless, establece la política de sesiones,
 * gestiona la autenticación mediante {@link AuthenticationManager} y provee el
 * encriptador de contraseñas {@link PasswordEncoder}.
 * </p>
 */

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Configura las reglas de acceso HTTP, deshabilita CSRF, define la política stateless
     * e inyecta el filtro de autenticación JWT antes del filtro de usuario/contraseña.
     *
     * @param http Instancia de {@link HttpSecurity} para construir la configuración.
     * @param jwtAuthFilter Filtro personalizado que intercepta y valida los tokens JWT.
     * @return La cadena de filtros de seguridad construida.
     * @throws Exception Si ocurre algún error en la construcción de la configuración de seguridad.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Desactivado para APIs REST Stateless
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos de autenticación y documentación
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Cualquier otra ruta requiere token válido
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Sin sesiones de servidor
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Proporciona el componente de codificación de contraseñas.
     *
     * @return Una instancia de {@link BCryptPasswordEncoder} con fuerza por defecto de 10.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();//Fuerza por defecto: 10
    }

    /**
     * Expone el gestor de autenticación de Spring Security requerido para autenticar credenciales.
     *
     * @param config Configuración de autenticación del contenedor de Spring.
     * @return La instancia de {@link AuthenticationManager}.
     * @throws Exception Si no se logra obtener el administrador de autenticación.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}