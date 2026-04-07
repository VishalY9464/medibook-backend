package com.medibook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * This exception is thrown when someone tries to create
 * something that already exists in the database.
 *
 * Think of it like trying to register with an email
 * that is already taken.
 * "Sorry this email is already registered."
 *
 * When to throw this:
 * → Email already registered (Auth)
 * → Provider profile already exists for this user (Provider)
 * → Payment already exists for this appointment (Payment)
 * → Review already exists for this appointment (Review)
 * → Medical record already exists for appointment (Record)
 *
 * Gives 409 Conflict response instead of ugly 500 error.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    /*
     * Constructor that builds a clean conflict message.
     *
     * Example usage:
     * throw new DuplicateResourceException(
     *     "Provider profile", "userId", userId
     * );
     *
     * Produces message:
     * "Provider profile already exists with userId: 1"
     */
    public DuplicateResourceException(
            String resourceName,
            String fieldName,
            Object fieldValue) {

        super(String.format(
            "%s already exists with %s: %s",
            resourceName,
            fieldName,
            fieldValue
        ));
    }

    /*
     * Simple message constructor.
     * Use when you want full control over message.
     *
     * Example:
     * throw new DuplicateResourceException(
     *     "Payment already exists for appointment: " + appointmentId
     * );
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}