package com.streamvault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamvault.dto.AuthResponse;
import com.streamvault.dto.LoginRequest;
import com.streamvault.dto.SignupRequest;
import com.streamvault.exception.BadRequestException;
import com.streamvault.model.User;
import com.streamvault.security.JwtFilter;
import com.streamvault.security.JwtUtil;
import com.streamvault.security.LoginAttemptLimiter;
import com.streamvault.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(value = AuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class))
@Import(LoginAttemptLimiter.class) // real limiter; tests use distinct usernames since its state persists
@DisplayName("AuthController Tests")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private JwtUtil jwtUtil;

        @Nested
        @DisplayName("POST /api/auth/signup")
        class SignupEndpoint {

                @Test
                @WithMockUser
                @DisplayName("Should return 200 with auth response on successful signup")
                void signup_success() throws Exception {
                        SignupRequest request = new SignupRequest();
                        request.setUsername("newuser");
                        request.setEmail("new@example.com");
                        request.setPassword("password123");

                        AuthResponse response = AuthResponse.builder()
                                        .token("jwt-token")
                                        .username("newuser")
                                        .email("new@example.com")
                                        .message("Signup successful")
                                        .build();

                        when(authService.signup(any(SignupRequest.class))).thenReturn(response);

                        mockMvc.perform(post("/api/auth/signup")
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.token").value("jwt-token"))
                                        .andExpect(jsonPath("$.username").value("newuser"))
                                        .andExpect(jsonPath("$.email").value("new@example.com"))
                                        .andExpect(jsonPath("$.message").value("Signup successful"));
                }

                @Test
                @WithMockUser
                @DisplayName("Should return 400 when username already exists")
                void signup_duplicateUsername() throws Exception {
                        SignupRequest request = new SignupRequest();
                        request.setUsername("existing");
                        request.setEmail("new@example.com");
                        request.setPassword("password");

                        when(authService.signup(any(SignupRequest.class)))
                                        .thenThrow(new BadRequestException("Username already exists"));

                        mockMvc.perform(post("/api/auth/signup")
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.error").value("Username already exists"));
                }

                @Test
                @WithMockUser
                @DisplayName("Should return 400 without calling the service for invalid input")
                void signup_invalidInput() throws Exception {
                        String[] bodies = {
                                        "{\"username\":\"ok_user\",\"email\":\"a@b.com\",\"password\":\"short\"}",
                                        "{\"username\":\"ok_user\",\"email\":\"not-an-email\",\"password\":\"password123\"}",
                                        "{\"username\":\"x\",\"email\":\"a@b.com\",\"password\":\"password123\"}",
                                        "{\"username\":\"bad name!\",\"email\":\"a@b.com\",\"password\":\"password123\"}",
                                        "{\"email\":\"a@b.com\",\"password\":\"password123\"}",
                        };
                        for (String body : bodies) {
                                mockMvc.perform(post("/api/auth/signup")
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                                .andExpect(status().isBadRequest())
                                                .andExpect(jsonPath("$.error").exists());
                        }
                        verify(authService, never()).signup(any());
                }
        }

        @Nested
        @DisplayName("POST /api/auth/login")
        class LoginEndpoint {

                @Test
                @WithMockUser
                @DisplayName("Should return 200 with auth response on successful login")
                void login_success() throws Exception {
                        LoginRequest request = new LoginRequest();
                        request.setUsername("testuser");
                        request.setPassword("password123");

                        AuthResponse response = AuthResponse.builder()
                                        .token("jwt-token-login")
                                        .username("testuser")
                                        .email("test@example.com")
                                        .message("Login successful")
                                        .build();

                        when(authService.login(any(LoginRequest.class))).thenReturn(response);

                        mockMvc.perform(post("/api/auth/login")
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.token").value("jwt-token-login"))
                                        .andExpect(jsonPath("$.username").value("testuser"))
                                        .andExpect(jsonPath("$.message").value("Login successful"));
                }

                @Test
                @WithMockUser
                @DisplayName("Should return 400 on invalid credentials")
                void login_invalidCredentials() throws Exception {
                        LoginRequest request = new LoginRequest();
                        request.setUsername("testuser");
                        request.setPassword("wrong");

                        when(authService.login(any(LoginRequest.class)))
                                        .thenThrow(new BadRequestException("Invalid credentials"));

                        mockMvc.perform(post("/api/auth/login")
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.error").value("Invalid credentials"));
                }

                @Test
                @WithMockUser
                @DisplayName("Should return 429 after repeated failed logins for the same user")
                void login_rateLimited() throws Exception {
                        LoginRequest request = new LoginRequest();
                        request.setUsername("victim");
                        request.setPassword("guess");

                        when(authService.login(any(LoginRequest.class)))
                                        .thenThrow(new BadRequestException("Invalid credentials"));

                        for (int i = 0; i < 5; i++) {
                                mockMvc.perform(post("/api/auth/login")
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                                .andExpect(status().isBadRequest());
                        }
                        mockMvc.perform(post("/api/auth/login")
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isTooManyRequests());
                }
        }

        @Nested
        @DisplayName("GET /api/auth/me")
        class MeEndpoint {

                @Test
                @WithMockUser(username = "testuser")
                @DisplayName("Should return current user info when authenticated")
                void me_authenticated() throws Exception {
                        User user = User.builder()
                                        .id(1L)
                                        .username("testuser")
                                        .email("test@example.com")
                                        .password("encoded")
                                        .build();

                        when(authService.getUserByUsername("testuser")).thenReturn(user);

                        mockMvc.perform(get("/api/auth/me"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.username").value("testuser"))
                                        .andExpect(jsonPath("$.email").value("test@example.com"));
                }

                @Test
                @DisplayName("Should return 401 when not authenticated")
                void me_unauthenticated() throws Exception {
                        mockMvc.perform(get("/api/auth/me"))
                                        .andExpect(status().isUnauthorized());
                }
        }
}
