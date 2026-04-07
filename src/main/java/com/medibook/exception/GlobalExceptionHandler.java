package com.medibook.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/*
 * This is the ONE place that catches ALL exceptions.
 *
 * Think of it like a hospital complaint desk.
 * Every department (service) sends complaints here.
 * This desk formats them nicely and sends back to patient.
 *
 * @RestControllerAdvice → intercepts all exceptions
 * from all controllers in entire application.
 *
 * Without this → ugly 500 stack trace exposed to client.
 * With this → clean JSON error response always.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * Handle 404 — resource not found.
     * Thrown by all services when entity not found in DB.
     * Example: Slot not found with id: 5
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    /*
     * Handle 409 — duplicate resource conflict.
     * Thrown when trying to create something that exists.
     * Example: Email already registered.
     * Example: Review already exists for appointment.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    /*
     * Handle 400 — bad request / invalid business rule.
     * Thrown when request violates business logic.
     * Example: Cannot book already booked slot.
     * Example: Cannot cancel completed appointment.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    /*
     * Handle 401 — unauthorized / not logged in.
     * Thrown when user is not authenticated.
     * Example: Invalid credentials.
     * Example: Token expired.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            ex.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    /*
     * Handle 403 — forbidden / no permission.
     * Thrown when user is logged in but lacks permission.
     * Example: Patient accessing admin panel.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            ex.getMessage(),
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error);
    }

    /*
     * Handle 400 — validation errors from @Valid.
     * Thrown when request body fails @NotNull @NotBlank etc.
     * Example: appointmentId is required.
     * Example: Rating must be between 1 and 5.
     * Returns all field errors at once.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // collect all field errors into a map
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult()
                .getFieldErrors()) {
            fieldErrors.put(
                fieldError.getField(),
                fieldError.getDefaultMessage()
            );
        }

        // build response with all validation errors
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", fieldErrors);
        response.put("path", request.getRequestURI());
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /*
     * Handle 500 — any unexpected exception.
     * Catches everything not handled above.
     * Last line of defense — no stack trace exposed.
     * Logs the error for debugging.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        // log for debugging — never expose to client
        System.err.println("Unexpected error: "
                + ex.getMessage());
        ex.printStackTrace();

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "Something went wrong. Please try again later.",
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}