package com.medibook.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String phone;

    // Only Patient or Provider allowed
    // Admin is created manually in database
    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "Patient|Provider",
        message = "Role must be Patient or Provider"
    )
    private String role;
}
//}
//```
//
//**Why each field:**
//```
//fullName   → @NotBlank = cannot be empty
//email      → @Email = must be valid email format
//password   → @Size min 6 = minimum 6 characters
//phone      → optional, no validation
//role       → @Pattern = only accepts "Patient" or "Provider"
//             Admin cannot self-register
//             Admin is created directly in database