package com.medibook.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String passwordHash;

    private String phone;

    // Patient / Provider / Admin
    @Column(nullable = false)
    private String role;

    // google / github / null for normal login
    private String provider;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String profilePicUrl;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}


//
//**Why each field — directly from PDF:**
//userId        → unique ID for every user
//fullName      → person's name
//email         → unique, used for login
//passwordHash  → bcrypt hashed password (PDF requires bcrypt)
//phone         → contact number
//role          → Patient / Provider / Admin (3 roles from PDF)
//provider      → google or github if OAuth login
//isActive      → false when admin suspends account
//createdAt     → auto set when user registers
//profilePicUrl → profile picture URL