package com.medibook.auth.service.impl;

import com.medibook.auth.dto.AuthResponse;

import com.medibook.auth.dto.LoginRequest;
import com.medibook.auth.dto.RegisterRequest;
import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;
import com.medibook.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    //  register()
    @Override
    public User register(RegisterRequest request) {

        // Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    //  login()
    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole(),
                user.getUserId()
        );

        return new AuthResponse(
                token,
                user.getRole(),
                user.getUserId(),
                user.getFullName(),
                "Login successful"
        );
    }

    // logout()
    @Override
    public void logout(String token) {
        // Stateless JWT — client discards token
        // Production: add token to Redis blacklist
    }

    // validateToken()
    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    //  refreshToken()
    @Override
    public String refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }
        String email  = jwtUtil.extractEmail(token);
        String role   = jwtUtil.extractRole(token);
        int userId    = jwtUtil.extractUserId(token);
        return jwtUtil.generateToken(email, role, userId);
    }

    // getUserByEmail()
    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    //  getUserById()
    @Override
    public User getUserById(int userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // updateProfile()
    @Override
    public User updateProfile(int userId, User updatedUser) {
        User existing = getUserById(userId);
        existing.setFullName(updatedUser.getFullName());
        existing.setPhone(updatedUser.getPhone());
        existing.setProfilePicUrl(updatedUser.getProfilePicUrl());
        return userRepository.save(existing);
    }

    //  changePassword()
    @Override
    public void changePassword(int userId, String newPassword) {
        User user = getUserById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    //  deactivateAccount()
    @Override
    public void deactivateAccount(int userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }
}

//**What this class does:
//register()         → checks duplicate email, bcrypt hashes password, saves user
//login()            → finds user, checks active, validates password, returns JWT
//logout()           → stateless JWT, client just discards token
//validateToken()    → checks if JWT is valid
//refreshToken()     → generates new JWT from old one
//getUserByEmail()   → finds user by email
//getUserById()      → finds user by ID
//updateProfile()    → updates name, phone, profilePic
//changePassword()   → bcrypt hashes new password, saves
//deactivateAccount()→ sets isActive = false (soft delete)