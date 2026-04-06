package com.medibook.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/*
 * DTO for submitting a review after appointment.
 * Patient fills this after doctor marks appointment COMPLETED.
 * PDF rule: only one review per appointment allowed.
 */
@Data
public class ReviewRequest {

    /*
     * Which appointment is this review for?
     * Appointment must be COMPLETED status.
     * One appointment = one review only.
     */
    @NotNull(message = "Appointment ID is required")
    private int appointmentId;

    /*
     * Who is submitting this review?
     * Must be the patient who had the appointment.
     */
    @NotNull(message = "Patient ID is required")
    private int patientId;

    /*
     * Which doctor is being reviewed?
     * Links to Provider entity from UC2.
     * Used to update doctor avgRating automatically.
     */
    @NotNull(message = "Provider ID is required")
    private int providerId;

    /*
     * Star rating given by patient.
     * PDF defines: 1 to 5 stars only.
     * 1 = very poor, 5 = excellent.
     * Used to calculate doctor average rating.
     */
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private int rating;

    /*
     * Written feedback from patient.
     * Optional — patient can leave blank.
     * Example: "Very helpful doctor. Explained everything clearly."
     */
    private String comment;

    /*
     * Should patient name be hidden?
     * true  = show as "Anonymous Patient"
     * false = show actual patient name
     * PDF requires this option for patient privacy.
     */
    private boolean isAnonymous = false;
}