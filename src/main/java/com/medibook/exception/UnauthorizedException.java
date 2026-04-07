package com.medibook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * This exception is thrown when user is not logged in
 * but tries to access a protected resource.
 *
 * Think of it like trying to enter a hospital ward
 * without showing your ID card at reception.
 * "Sorry you need to login first to access this."
 *
 * When to throw this:
 * → No JWT token in request header
 * → JWT token is expired
 * → JWT token is invalid or tampered
 * → User account is deactivated
 *
 * Gives 401 Unauthorized instead of ugly 500 error.
 *
 * Difference between 401 and 403:
 * 401 Unauthorized → you are NOT logged in
 * 403 Forbidden    → you ARE logged in but no permission
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    /*
     * Constructor with custom message.
     *
     * Example usage:
     * throw new UnauthorizedException(
     *     "You must be logged in to access this resource."
     * );
     *
     * Produces clean 401 response:
     * {
     *   "status": 401,
     *   "error": "Unauthorized",
     *   "message": "You must be logged in to access this resource."
     * }
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}