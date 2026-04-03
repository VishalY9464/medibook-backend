package com.medibook.auth.resource;

import com.medibook.auth.dto.AuthResponse;
import com.medibook.auth.dto.LoginRequest;
import com.medibook.auth.dto.RegisterRequest;
import com.medibook.auth.entity.User;
import com.medibook.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthResource {

    @Autowired
    private AuthService authService;

    // PDF: /auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Registration successful",
                        "userId", user.getUserId(),
                        "role", user.getRole()
                ));
    }

    // PDF: /auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // PDF: /auth/logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // PDF: /auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String newToken = authService.refreshToken(token);
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    // PDF: /auth/profile GET
    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getProfile(@PathVariable int userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    // PDF: /auth/profile PUT
    @PutMapping("/profile/{userId}")
    public ResponseEntity<User> updateProfile(
            @PathVariable int userId,
            @RequestBody User updatedUser) {
        return ResponseEntity.ok(authService.updateProfile(userId, updatedUser));
    }

    // PDF: /auth/password
    @PutMapping("/password/{userId}")
    public ResponseEntity<?> changePassword(
            @PathVariable int userId,
            @RequestBody Map<String, String> body) {
        authService.changePassword(userId, body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // PDF: /auth/deactivate
    @PutMapping("/deactivate/{userId}")
    public ResponseEntity<?> deactivate(@PathVariable int userId) {
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully"));
    }
}

