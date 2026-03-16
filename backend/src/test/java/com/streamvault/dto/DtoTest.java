package com.streamvault.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Tests")
class DtoTest {

    @Nested
    @DisplayName("SignupRequest Tests")
    class SignupRequestTests {

        @Test
        @DisplayName("Should set and get all fields")
        void settersAndGetters() {
            SignupRequest req = new SignupRequest();
            req.setUsername("user1");
            req.setEmail("user1@test.com");
            req.setPassword("pass123");

            assertEquals("user1", req.getUsername());
            assertEquals("user1@test.com", req.getEmail());
            assertEquals("pass123", req.getPassword());
        }
    }

    @Nested
    @DisplayName("LoginRequest Tests")
    class LoginRequestTests {

        @Test
        @DisplayName("Should set and get all fields")
        void settersAndGetters() {
            LoginRequest req = new LoginRequest();
            req.setUsername("user1");
            req.setPassword("pass123");

            assertEquals("user1", req.getUsername());
            assertEquals("pass123", req.getPassword());
        }
    }

    @Nested
    @DisplayName("AuthResponse Tests")
    class AuthResponseTests {

        @Test
        @DisplayName("Should create via builder")
        void builderTest() {
            AuthResponse resp = AuthResponse.builder()
                    .token("tok")
                    .username("user")
                    .email("e@e.com")
                    .message("ok")
                    .build();

            assertEquals("tok", resp.getToken());
            assertEquals("user", resp.getUsername());
            assertEquals("e@e.com", resp.getEmail());
            assertEquals("ok", resp.getMessage());
        }

        @Test
        @DisplayName("Should create via no-arg constructor and setters")
        void noArgAndSetters() {
            AuthResponse resp = new AuthResponse();
            resp.setToken("t");
            resp.setUsername("u");
            resp.setEmail("e");
            resp.setMessage("m");

            assertEquals("t", resp.getToken());
            assertEquals("u", resp.getUsername());
            assertEquals("e", resp.getEmail());
            assertEquals("m", resp.getMessage());
        }

        @Test
        @DisplayName("Should create via all-arg constructor")
        void allArgConstructor() {
            AuthResponse resp = new AuthResponse("tok", "user", "e@e.com", "msg");

            assertEquals("tok", resp.getToken());
            assertEquals("user", resp.getUsername());
            assertEquals("e@e.com", resp.getEmail());
            assertEquals("msg", resp.getMessage());
        }
    }

    @Nested
    @DisplayName("VideoResponse Tests")
    class VideoResponseTests {

        @Test
        @DisplayName("Should create via builder")
        void builderTest() {
            LocalDateTime now = LocalDateTime.now();
            VideoResponse resp = VideoResponse.builder()
                    .id(1L)
                    .title("vid")
                    .contentType("video/mp4")
                    .size(1024L)
                    .uploadDate(now)
                    .build();

            assertEquals(1L, resp.getId());
            assertEquals("vid", resp.getTitle());
            assertEquals("video/mp4", resp.getContentType());
            assertEquals(1024L, resp.getSize());
            assertEquals(now, resp.getUploadDate());
        }

        @Test
        @DisplayName("Should create via no-arg constructor and setters")
        void noArgAndSetters() {
            VideoResponse resp = new VideoResponse();
            resp.setId(5L);
            resp.setTitle("t");
            resp.setContentType("video/webm");
            resp.setSize(512L);
            LocalDateTime date = LocalDateTime.of(2026, 1, 1, 0, 0);
            resp.setUploadDate(date);

            assertEquals(5L, resp.getId());
            assertEquals("t", resp.getTitle());
            assertEquals("video/webm", resp.getContentType());
            assertEquals(512L, resp.getSize());
            assertEquals(date, resp.getUploadDate());
        }
    }
}
