package com.musa.communities_service.polls.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class PollSecurityConfig {

    @Bean
    public SecurityFilterChain pollSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http
                .securityMatcher(
                        "/api/v1/polls/**",
                        "/api/v1/communities/*/polls",
                        "/api/v1/communities/*/polls/**"
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/polls/ws").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, error) -> {
                            response.setStatus(401);
                            response.setHeader("WWW-Authenticate", "Bearer");
                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("""
                                    {
                                      "code": "UNAUTHORIZED",
                                      "message": "Inicia sesión nuevamente."
                                    }
                                    """);
                        })
                        .accessDeniedHandler((request, response, error) -> {
                            response.setStatus(403);
                            response.setContentType(
                                    MediaType.APPLICATION_JSON_VALUE
                            );
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("""
                                    {
                                      "code": "FORBIDDEN",
                                      "message": "No tienes permiso para esta operación."
                                    }
                                    """);
                        })
                )
                .build();
    }
}