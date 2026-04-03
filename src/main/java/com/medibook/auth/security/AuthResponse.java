package com.medibook.auth.security;

import lombok.AllArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    // JWT token — sent back to client after login
    private String token;

    // Role — Patient / Provider / Admin
    // Client uses this to redirect to correct dashboard
    private String role;

    // userId — client stores this for future API calls
    private int userId;

    // fullName — to display on dashboard
    private String fullName;

    // message — success or error message
    private String message;
}
//```
//
//**Why each field:**
//```
//token     → JWT token client must send in every
//            future request as:
//            Authorization: Bearer <token>
//
//role      → tells frontend which dashboard to show
//            Patient   → patient dashboard
//            Provider  → provider dashboard
//            Admin     → admin dashboard
//
//userId    → client stores this to make profile
//            calls, appointment calls etc
//
//fullName  → display name on dashboard header
//
//message   → "Login successful" or error details