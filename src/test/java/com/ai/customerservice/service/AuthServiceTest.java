package com.ai.customerservice.service;

import com.ai.customerservice.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        AuthProperties.User user1 = new AuthProperties.User();
        user1.setUsername("admin");
        user1.setPassword("admin123");
        AuthProperties.User user2 = new AuthProperties.User();
        user2.setUsername("user");
        user2.setPassword("user123");
        props.setUsers(List.of(user1, user2));

        authService = new AuthService(props);
    }

    @Test
    void login_withCorrectCredentials_shouldReturnToken() {
        Optional<String> token = authService.login("admin", "admin123");

        assertTrue(token.isPresent());
        assertFalse(token.get().isEmpty());
    }

    @Test
    void login_withWrongPassword_shouldReturnEmpty() {
        Optional<String> token = authService.login("admin", "wrong");

        assertTrue(token.isEmpty());
    }

    @Test
    void login_withWrongUsername_shouldReturnEmpty() {
        Optional<String> token = authService.login("nonexistent", "admin123");

        assertTrue(token.isEmpty());
    }

    @Test
    void login_withNullUsername_shouldReturnEmpty() {
        Optional<String> token = authService.login(null, "admin123");
        assertTrue(token.isEmpty());
    }

    @Test
    void login_withNullPassword_shouldReturnEmpty() {
        Optional<String> token = authService.login("admin", null);
        assertTrue(token.isEmpty());
    }

    @Test
    void resolveUsername_withValidToken_shouldReturnUsername() {
        String token = authService.login("admin", "admin123").orElseThrow();

        Optional<String> username = authService.resolveUsername(token);

        assertTrue(username.isPresent());
        assertEquals("admin", username.get());
    }

    @Test
    void resolveUsername_withInvalidToken_shouldReturnEmpty() {
        Optional<String> username = authService.resolveUsername("invalid-token");

        assertTrue(username.isEmpty());
    }

    @Test
    void resolveUsername_withNullToken_shouldReturnEmpty() {
        Optional<String> username = authService.resolveUsername(null);
        assertTrue(username.isEmpty());
    }

    @Test
    void resolveUsername_withEmptyToken_shouldReturnEmpty() {
        Optional<String> username = authService.resolveUsername("");
        assertTrue(username.isEmpty());
    }

    @Test
    void logout_shouldInvalidateToken() {
        String token = authService.login("admin", "admin123").orElseThrow();

        authService.logout(token);

        Optional<String> username = authService.resolveUsername(token);
        assertTrue(username.isEmpty());
    }

    @Test
    void logout_withNullToken_shouldNotThrow() {
        assertDoesNotThrow(() -> authService.logout(null));
    }

    @Test
    void login_shouldReturnDifferentTokensEachTime() {
        String token1 = authService.login("admin", "admin123").orElseThrow();
        String token2 = authService.login("admin", "admin123").orElseThrow();

        assertNotEquals(token1, token2);
    }

    @Test
    void login_multipleUsers_shouldResolveCorrectly() {
        String token1 = authService.login("admin", "admin123").orElseThrow();
        String token2 = authService.login("user", "user123").orElseThrow();

        assertEquals("admin", authService.resolveUsername(token1).orElseThrow());
        assertEquals("user", authService.resolveUsername(token2).orElseThrow());
    }
}
