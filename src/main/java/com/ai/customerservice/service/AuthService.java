package com.ai.customerservice.service;

import com.ai.customerservice.config.AuthProperties;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    public static final String COOKIE_NAME = "AUTH_TOKEN";
    public static final int COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    private final AuthProperties authProperties;
    private final Map<String, String> tokenToUsername = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public AuthService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public Optional<String> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        boolean matched = authProperties.getUsers().stream()
                .anyMatch(u -> username.equals(u.getUsername()) && password.equals(u.getPassword()));
        if (!matched) {
            return Optional.empty();
        }
        String token = generateToken();
        tokenToUsername.put(token, username);
        return Optional.of(token);
    }

    public Optional<String> resolveUsername(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tokenToUsername.get(token));
    }

    public void logout(String token) {
        if (token != null) {
            tokenToUsername.remove(token);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
