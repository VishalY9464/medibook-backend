package com.medibook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * This exception is thrown when client sends invalid data
 * or tries to do something that violates business rules.
 *
 * Think of it like filling a form incorrectly.
 * "Sorry you cannot book a slot that is already booked."
 * "Sorry you cannot cancel a completed appointment."
 * "Sorry you cannot create a slot in the past."
 *
 * When to throw this:
 * → Trying to book already booked slot
 * → Trying to cancel completed appointment
 * → Trying to create slot in the past
 * → Trying to pay for cancelled appointment
 * → Trying to review before appointment completed
 * → Trying to create medical record for scheduled appointment
 * → Invalid date range for recurring slots
 *
 * Gives 400 Bad Request instead of ugly 500 error.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    /*
     * Constructor with custom message.
     *
     * Example usage:
     * throw new BadRequestException(
     *     "Cannot book a slot that is already booked."
     * );
     *
     * Produces clean 400 response:
     * {
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "Cannot book a slot that is already booked."
     * }
     */
    public BadRequestException(String message) {
        super(message);
    }
}