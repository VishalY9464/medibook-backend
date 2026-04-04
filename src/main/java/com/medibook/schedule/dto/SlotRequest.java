package com.medibook.schedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/*
 * This is the DTO for creating an availability slot.
 *
 * Think of it like a doctor filling in their diary.
 * Doctor says: "On April 5th, I am available from 10:00 to 10:30"
 * That is one slot.
 *
 * Why a DTO and not directly the entity?
 * We never expose our database entity to outside world.
 * DTO carries only what doctor needs to send — nothing more.
 */
@Data
public class SlotRequest {

    /*
     * Which doctor is creating this slot?
     * providerId comes from Provider entity (UC2).
     * Doctor must have a verified provider profile first.
     */
    @NotNull(message = "Provider ID is required")
    private int providerId;

    /*
     * Which date is this slot for?
     * Example: 2026-04-05
     * Cannot be null — every slot needs a specific date.
     */
    @NotNull(message = "Date is required")
    private LocalDate date;

    /*
     * What time does this slot start?
     * Example: 10:00
     * Cannot be null — slot needs a start time.
     */
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    /*
     * What time does this slot end?
     * Example: 10:30
     * Cannot be null — slot needs an end time.
     */
    @NotNull(message = "End time is required")
    private LocalTime endTime;

    /*
     * How long is this appointment in minutes?
     * Example: 30 minutes
     * Minimum 1 minute — cannot be zero or negative.
     */
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int durationMinutes;

    /*
     * Is this a recurring slot?
     * Values: NONE / DAILY / WEEKLY
     * NONE    → one time slot on specific date
     * DAILY   → repeat every day
     * WEEKLY  → repeat every week same day
     * Used in generateRecurringSlots() method.
     */
    private String recurrence = "NONE";

    /*
     * For recurring slots — what date to stop?
     * Example: doctor sets slots from April 1 to April 30
     * Only used when recurrence is DAILY or WEEKLY.
     * Optional — only needed for recurring slots.
     */
    private LocalDate recurrenceEndDate;
}