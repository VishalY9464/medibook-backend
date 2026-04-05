package com.medibook.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/*
 * This is the DTO for creating an appointment.
 *
 * Think of it like the booking form a patient fills
 * when they want to see a doctor.
 * Patient selects doctor, slot, date, time,
 * type of service, and how they want to consult.
 *
 * Why a DTO and not directly the Appointment entity?
 * We never expose database entities to outside world.
 * DTO carries only what patient needs to send.
 * Server side fields like status, createdAt are
 * set internally — patient should not control those.
 */
@Data
public class AppointmentRequest {

    /*
     * Which patient is booking this appointment?
     * patientId is the userId from User entity (UC1).
     * Patient must be logged in and have role=Patient.
     */
    @NotNull(message = "Patient ID is required")
    private int patientId;

    /*
     * Which doctor is being booked?
     * providerId comes from Provider entity (UC2).
     * Doctor must be verified and available.
     */
    @NotNull(message = "Provider ID is required")
    private int providerId;

    /*
     * Which specific time slot is being booked?
     * slotId comes from AvailabilitySlot entity (UC3).
     * Slot must be available — not booked, not blocked.
     */
    @NotNull(message = "Slot ID is required")
    private int slotId;

    /*
     * What type of service does patient need?
     * Example: General Consultation, Follow-Up,
     *          Dental Checkup, Blood Test
     * Helps doctor prepare before appointment.
     */
    @NotBlank(message = "Service type is required")
    private String serviceType;

    /*
     * What date is this appointment on?
     * Must match the date of the selected slot.
     * Example: 2026-04-10
     */
    @NotNull(message = "Appointment date is required")
    private LocalDate appointmentDate;

    /*
     * What time does appointment start?
     * Must match the startTime of selected slot.
     * Example: 10:00
     */
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    /*
     * What time does appointment end?
     * Must match the endTime of selected slot.
     * Example: 10:30
     */
    @NotNull(message = "End time is required")
    private LocalTime endTime;

    /*
     * How does patient want to consult?
     * IN_PERSON      → patient visits clinic physically
     * TELECONSULTATION → video call from home
     * PDF defines these two modes exactly.
     */
    @NotBlank(message = "Mode of consultation is required")
    private String modeOfConsultation;

    /*
     * Any additional notes from patient?
     * Example: "I have been having chest pain for 3 days"
     * Optional — helps doctor understand issue before appointment.
     */
    private String notes;
}