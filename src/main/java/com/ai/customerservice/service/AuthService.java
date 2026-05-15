package com.ai.customerservice.service;

import com.ai.customerservice.config.AuthProperties;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    public static final String COOKIE_NAME = "AUTH_TOKEN";
    public static final int COOKIE_MAX_AGE_SECONDS = 60 * 60;

    private final AuthProperties authProperties;
    private final Cache<String, String> tokenCache;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.tokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(COOKIE_MAX_AGE_SECONDS, TimeUnit.SECONDS)
                .build();
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
        tokenCache.put(token, username);
        return Optional.of(token);
    }

    public Optional<String> resolveUsername(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tokenCache.getIfPresent(token));
    }

    public void logout(String token) {
        if (token != null) {
            tokenCache.invalidate(token);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
