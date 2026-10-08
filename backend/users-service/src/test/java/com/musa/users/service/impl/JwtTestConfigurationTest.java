package com.musa.users.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class JwtTestConfigurationTest {
    @Test
    void generatedTestKeyCanSignAndValidateTokens() throws Exception {
        var environment = new StandardEnvironment();
        RandomValuePropertySource.addToEnvironment(environment);
        environment.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        var key = environment.getRequiredProperty("application.security.jwt.secret-key");
        assertTrue(Base64.getDecoder().decode(key).length >= 32);
        var service = new JwtServiceImpl();
        ReflectionTestUtils.setField(service, "secretKey", key);
        ReflectionTestUtils.setField(service, "jwtExpiration", 300000L);
        var user = User.withUsername("jwt-test@example.invalid").password("").roles("USER").build();
        var token = service.generateAccessToken(user);
        assertEquals(user.getUsername(), service.extractUsername(token));
        assertTrue(service.isTokenValid(token, user));
        var other = User.withUsername("other@example.invalid").password("").roles("USER").build();
        assertFalse(service.isTokenValid(token, other));
    }
}
