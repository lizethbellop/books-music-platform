package com.musa.users.security;

import com.musa.users.service.JwtService;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import jakarta.servlet.FilterChain;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = mock(JwtService.class);
    private final UserDetailsService users = mock(UserDetailsService.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    private void bearer() { request.addHeader("Authorization", "Bearer token"); }
    private UserDetails account(boolean enabled) {
        return User.withUsername("ana@example.com").password("unused")
                .authorities("USUARIO").disabled(!enabled).build();
    }

    @Test void missingHeaderContinuesWithoutAuthentication() throws Exception {
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwt, users);
    }

    @Test void malformedTokenReturns401InsteadOfInternalError() throws Exception {
        bearer();
        when(jwt.extractUsername("token")).thenThrow(new MalformedJwtException("invalid"));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain, users);
    }

    @Test void unknownAccountReturns401() throws Exception {
        bearer();
        when(jwt.extractUsername("token")).thenReturn("ana@example.com");
        when(users.loadUserByUsername("ana@example.com")).thenThrow(new UsernameNotFoundException("missing"));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test void disabledAccountCannotUseOldToken() throws Exception {
        bearer();
        when(jwt.extractUsername("token")).thenReturn("ana@example.com");
        when(users.loadUserByUsername("ana@example.com")).thenReturn(account(false));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test void invalidTokenDoesNotAuthenticate() throws Exception {
        bearer();
        when(jwt.extractUsername("token")).thenReturn("ana@example.com");
        when(users.loadUserByUsername("ana@example.com")).thenReturn(account(true));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void validTokenAuthenticatesAndContinues() throws Exception {
        bearer();
        var user = account(true);
        when(jwt.extractUsername("token")).thenReturn("ana@example.com");
        when(users.loadUserByUsername("ana@example.com")).thenReturn(user);
        when(jwt.isTokenValid("token", user)).thenReturn(true);
        filter.doFilter(request, response, chain);
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(chain).doFilter(request, response);
    }
}
