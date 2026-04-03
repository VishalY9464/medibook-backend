package com.medibook.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Generate signing key from secret
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // PDF: generateToken()
    // Embeds email, role, userId inside token
    public String generateToken(String email, String role, int userId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract email from token
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    // Extract role from token
    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    // Extract userId from token
    public int extractUserId(String token) {
        return ((Number) getClaims(token).get("userId")).intValue();
    }

    // PDF: validateToken()
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Internal method to parse token
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}


//```
//
//**What each method does:**
//```
//generateToken()  → creates JWT with email + role + userId embedded
//                   expires in 24 hours (86400000ms from properties)
//
//extractEmail()   → reads email from token (used in JwtFilter)
//extractRole()    → reads role from token (Patient/Provider/Admin)
//extractUserId()  → reads userId from token
//
//validateToken()  → returns true if token is valid and not expired
//                   returns false if expired or tampered
//
//getClaims()      → internal parser — reads all data from token