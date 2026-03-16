package com.streamvault.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Must be at least 256-bit (32 bytes) for HMAC-SHA
    private static final String SECRET = "TestStreamVaultSuperSecretKeyForJWTTokenGeneration2024MustBeLongEnough";
    private static final long EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    @DisplayName("Should generate a non-null token")
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken("testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should extract username from token")
    void getUsernameFromToken_shouldReturnCorrectUsername() {
        String username = "john_doe";
        String token = jwtUtil.generateToken(username);

        String extracted = jwtUtil.getUsernameFromToken(token);
        assertEquals(username, extracted);
    }

    @Test
    @DisplayName("Should validate a valid token")
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("testuser");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("Should reject an invalid token")
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.jwt.token"));
    }

    @Test
    @DisplayName("Should reject an empty token")
    void validateToken_shouldReturnFalseForEmptyToken() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    @DisplayName("Should reject a null token")
    void validateToken_shouldReturnFalseForNullToken() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    @DisplayName("Should reject a tampered token")
    void validateToken_shouldReturnFalseForTamperedToken() {
        String token = jwtUtil.generateToken("testuser");
        // Tamper with the token by appending extra characters
        String tampered = token + "tampered";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    @DisplayName("Should reject token signed with different secret")
    void validateToken_shouldReturnFalseForDifferentSecret() {
        // Generate with one JwtUtil having a different secret
        JwtUtil otherJwtUtil = new JwtUtil(
                "AnotherCompletelyDifferentSecretKeyThatIsLongEnoughForHmacSha256", EXPIRATION);
        String token = otherJwtUtil.generateToken("testuser");

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("Should reject expired token")
    void validateToken_shouldReturnFalseForExpiredToken() {
        // Create a JwtUtil with 0ms expiration (already expired)
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, 0L);
        String token = expiredJwtUtil.generateToken("testuser");

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void generateToken_shouldGenerateDifferentTokensForDifferentUsers() {
        String token1 = jwtUtil.generateToken("user1");
        String token2 = jwtUtil.generateToken("user2");

        assertNotEquals(token1, token2);
    }
}
