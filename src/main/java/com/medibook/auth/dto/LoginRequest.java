package com.medibook.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
//```
//
//**Why each field:**
//```
//email     → @NotBlank = cannot be empty
//            @Email    = must be valid email format
//
//password  → @NotBlank = cannot be empty
//            no @Size here because we are just
//            checking against stored hash
//            not creating new password