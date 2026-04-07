package com.medibook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * This exception is thrown when something is not found
 * in the database.
 *
 * Think of it like a "record not found" message
 * at a hospital reception.
 * "Sorry we have no patient with ID 5 in our system."
 *
 * When to throw this:
 * → Slot not found with id: 5
 * → Appointment not found with id: 10
 * → Provider not found with id: 3
 * → User not found with email: test@test.com
 * → Medical record not found for appointment: 2
 *
 * Why a custom exception instead of RuntimeException?
 * → RuntimeException gives ugly 500 error
 * → This gives clean 404 Not Found response
 * → GlobalExceptionHandler catches this specifically
 * → Professional API design pattern
 *
 * @ResponseStatus tells Spring this is a 404 error.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /*
     * Resource name — what type of thing was not found?
     * Example: Slot, Appointment, Provider, User
     */
    private String resourceName;

    /*
     * Field name — what field was used to search?
     * Example: id, email, slotId, appointmentId
     */
    private String fieldName;

    /*
     * Field value — what value was searched for?
     * Example: 5, test@test.com, 10
     */
    private Object fieldValue;

    /*
     * Constructor that builds a clean error message.
     *
     * Example usage in service:
     * throw new ResourceNotFoundException("Slot", "id", slotId);
     *
     * Produces message:
     * "Slot not found with id: 5"
     *
     * This replaces all these ugly lines across services:
     * throw new RuntimeException("Slot not found with id: " + slotId);
     */
    public ResourceNotFoundException(
            String resourceName,
            String fieldName,
            Object fieldValue) {

        // call parent constructor with formatted message
        super(String.format(
            "%s not found with %s: %s",
            resourceName,
            fieldName,
            fieldValue
        ));

        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    // getters for GlobalExceptionHandler to use
    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}