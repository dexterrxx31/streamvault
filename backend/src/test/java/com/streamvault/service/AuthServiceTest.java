package com.streamvault.service;

import com.streamvault.dto.AuthResponse;
import com.streamvault.dto.LoginRequest;
import com.streamvault.dto.SignupRequest;
import com.streamvault.model.User;
import com.streamvault.repository.UserRepository;
import com.streamvault.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();
    }

    @Nested
    @DisplayName("Signup Tests")
    class SignupTests {

        private SignupRequest createSignupRequest(String username, String email, String password) {
            SignupRequest request = new SignupRequest();
            request.setUsername(username);
            request.setEmail(email);
            request.setPassword(password);
            return request;
        }

        @Test
        @DisplayName("Should signup successfully with valid request")
        void signup_success() {
            SignupRequest request = createSignupRequest("newuser", "new@example.com", "password123");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(jwtUtil.generateToken("newuser")).thenReturn("jwt-token-123");

            AuthResponse response = authService.signup(request);

            assertNotNull(response);
            assertEquals("jwt-token-123", response.getToken());
            assertEquals("newuser", response.getUsername());
            assertEquals("new@example.com", response.getEmail());
            assertEquals("Signup successful", response.getMessage());

            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("password123");
            verify(jwtUtil).generateToken("newuser");
        }

        @Test
        @DisplayName("Should throw when username already exists")
        void signup_duplicateUsername() {
            SignupRequest request = createSignupRequest("existinguser", "new@test.com", "pass");

            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.signup(request));
            assertEquals("Username already exists", exception.getMessage());

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when email already exists")
        void signup_duplicateEmail() {
            SignupRequest request = createSignupRequest("newuser", "existing@test.com", "pass");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.signup(request));
            assertEquals("Email already exists", exception.getMessage());

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        private LoginRequest createLoginRequest(String username, String password) {
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword(password);
            return request;
        }

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_success() {
            LoginRequest request = createLoginRequest("testuser", "password123");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            when(jwtUtil.generateToken("testuser")).thenReturn("jwt-token-456");

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("jwt-token-456", response.getToken());
            assertEquals("testuser", response.getUsername());
            assertEquals("test@example.com", response.getEmail());
            assertEquals("Login successful", response.getMessage());
        }

        @Test
        @DisplayName("Should throw when user not found")
        void login_userNotFound() {
            LoginRequest request = createLoginRequest("nonexistent", "password");

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.login(request));
            assertEquals("Invalid credentials", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw when password does not match")
        void login_wrongPassword() {
            LoginRequest request = createLoginRequest("testuser", "wrongPassword");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.login(request));
            assertEquals("Invalid credentials", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("GetUserByUsername Tests")
    class GetUserByUsernameTests {

        @Test
        @DisplayName("Should return user when found")
        void getUserByUsername_found() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            User user = authService.getUserByUsername("testuser");

            assertNotNull(user);
            assertEquals("testuser", user.getUsername());
            assertEquals("test@example.com", user.getEmail());
        }

        @Test
        @DisplayName("Should throw when user not found")
        void getUserByUsername_notFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.getUserByUsername("unknown"));
            assertEquals("User not found", exception.getMessage());
        }
    }
}
