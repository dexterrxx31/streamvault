package com.streamvault.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Model Tests")
class UserTest {

    @Test
    @DisplayName("Should create User using builder")
    void builder_createsUser() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("secret")
                .build();

        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
    }

    @Test
    @DisplayName("Should create User with no-arg constructor")
    void noArgConstructor() {
        User user = new User();
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertNull(user.getPassword());
    }

    @Test
    @DisplayName("Should set and get fields via setters/getters")
    void settersAndGetters() {
        User user = new User();
        user.setId(5L);
        user.setUsername("setter_user");
        user.setEmail("setter@test.com");
        user.setPassword("pass123");

        assertEquals(5L, user.getId());
        assertEquals("setter_user", user.getUsername());
        assertEquals("setter@test.com", user.getEmail());
        assertEquals("pass123", user.getPassword());
    }

    @Test
    @DisplayName("Should create User with all-arg constructor")
    void allArgConstructor() {
        User user = new User(10L, "allarg", "allarg@test.com", "pwd");

        assertEquals(10L, user.getId());
        assertEquals("allarg", user.getUsername());
        assertEquals("allarg@test.com", user.getEmail());
        assertEquals("pwd", user.getPassword());
    }
}
