package com.medibook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * Thrown when user is logged in but has no permission.
 * Example: Patient trying to access admin panel.
 * Example: Doctor trying to view another doctor's records.
 * Gives 403 Forbidden response.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}