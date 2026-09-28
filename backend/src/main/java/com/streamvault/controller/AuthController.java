package com.streamvault.controller;

import com.streamvault.dto.*;
import com.streamvault.exception.BadRequestException;
import com.streamvault.exception.TooManyRequestsException;
import com.streamvault.model.User;
import com.streamvault.security.LoginAttemptLimiter;
import com.streamvault.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public AuthController(AuthService authService, LoginAttemptLimiter loginAttemptLimiter) {
        this.authService = authService;
        this.loginAttemptLimiter = loginAttemptLimiter;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (loginAttemptLimiter.isBlocked(request.getUsername(), clientIp)) {
            throw new TooManyRequestsException("Too many failed login attempts. Try again later.");
        }
        try {
            AuthResponse response = authService.login(request);
            loginAttemptLimiter.reset(request.getUsername(), clientIp);
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            loginAttemptLimiter.recordFailure(request.getUsername(), clientIp);
            throw e;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getCurrentUser(Authentication authentication) {
        User user = authService.getUserByUsername(authentication.getName());
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "email", user.getEmail()));
    }
}
