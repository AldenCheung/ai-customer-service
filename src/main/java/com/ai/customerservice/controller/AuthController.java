package com.ai.customerservice.controller;

import com.ai.customerservice.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String username = body.get("username");
        String password = body.get("password");
        Optional<String> token = authService.login(username, password);
        if (token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "用户名或密码错误"));
        }
        Cookie cookie = new Cookie(AuthService.COOKIE_NAME, token.get());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(AuthService.COOKIE_MAX_AGE_SECONDS);
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("success", true, "username", username));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractToken(request);
        authService.logout(token);
        Cookie cookie = new Cookie(AuthService.COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String token = extractToken(request);
        return authService.resolveUsername(token)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("username", u)))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "unauthorized")));
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AuthService.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
