package com.medibook.auth.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (not needed for REST APIs with JWT)
            .csrf(csrf -> csrf.disable())

            // Stateless session — no server side sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL access rules
            .authorizeHttpRequests(auth -> auth

                // Public endpoints — no token needed
                .requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/auth/refresh",
                    "/providers",
                    "/providers/**",
                    "/slots/available/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**"
                ).permitAll()

                // Patient only endpoints
                .requestMatchers(
                    "/appointments/**",
                    "/payments/**",
                    "/reviews/**",
                    "/records/patient/**"
                ).hasRole("Patient")

                // Provider only endpoints
                .requestMatchers(
                    "/slots/**",
                    "/records/provider/**"
                ).hasRole("Provider")

                // Admin only endpoints
                .requestMatchers(
                    "/admin/**"
                ).hasRole("Admin")

                // Everything else needs authentication
                .anyRequest().authenticated()
            )

            // Add JWT filter before Spring Security filter
            .addFilterBefore(jwtFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // BCrypt password encoder — PDF requires bcrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}


//```
//
//**What each section does:**
//```
//csrf.disable()          → REST APIs don't need CSRF protection
//                          JWT handles security instead
//
//STATELESS               → no HttpSession created
//                          every request must carry JWT token
//
//permitAll()             → these URLs work without login
//                          guests can browse providers and slots
//
//hasRole("Patient")      → only patients can book appointments,
//                          make payments, submit reviews
//
//hasRole("Provider")     → only providers can manage slots,
//                          view their records
//
//hasRole("Admin")        → only admin can access /admin/** endpoints
//
//BCryptPasswordEncoder   → PDF explicitly requires bcrypt for passwords
//
//JwtFilter               → runs before every request to validate token