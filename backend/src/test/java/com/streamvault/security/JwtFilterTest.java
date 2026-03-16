package com.streamvault.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter Tests")
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should set authentication when valid Bearer token is present")
    void doFilter_validToken() throws ServletException, IOException {
        String token = "valid-jwt-token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("testuser", SecurityContextHolder.getContext().getAuthentication().getName());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when token is invalid")
    void doFilter_invalidToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when no Authorization header")
    void doFilter_noHeader() throws ServletException, IOException {
        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not set authentication when Authorization header is not Bearer")
    void doFilter_nonBearerHeader() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should always call filterChain.doFilter regardless of token validity")
    void doFilter_alwaysContinuesChain() throws ServletException, IOException {
        // No auth header
        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);

        // With invalid token
        request.addHeader("Authorization", "Bearer bad");
        when(jwtUtil.validateToken("bad")).thenReturn(false);
        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(2)).doFilter(request, response);
    }
}
