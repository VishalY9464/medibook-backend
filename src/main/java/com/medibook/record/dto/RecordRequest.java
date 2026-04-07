package com.medibook.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/*
 * This is the DTO for creating a medical record.
 *
 * Think of it like the prescription pad a doctor fills
 * after completing a consultation.
 * Doctor writes diagnosis, prescribes medicines,
 * adds clinical notes, and sets a follow up date.
 *
 * Why a DTO and not directly the entity?
 * We never expose database entities to outside world.
 * DTO carries only what doctor needs to send.
 * Fields like createdAt and updatedAt are set internally.
 */
@Data
public class RecordRequest {

    /*
     * Which appointment is this record for?
     * Links medical record to the appointment.
     * One appointment can have only one medical record.
     * This is enforced by unique constraint in entity.
     */
    @NotNull(message = "Appointment ID is required")
    private int appointmentId;

    /*
     * Which patient is this record for?
     * Links record to patient so they can view it later.
     * Patient can only see their own records.
     */
    @NotNull(message = "Patient ID is required")
    private int patientId;

    /*
     * Which doctor created this record?
     * Links record to doctor who conducted consultation.
     * Doctor can only see records they created.
     */
    @NotNull(message = "Provider ID is required")
    private int providerId;

    /*
     * What is the diagnosis?
     * Example: Viral chest infection, Type 2 Diabetes
     * This is the main finding from the consultation.
     * Cannot be blank — every record needs a diagnosis.
     */
    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    /*
     * What medicines are prescribed?
     * Example: Amoxicillin 500mg twice daily for 5 days
     * Doctor writes full prescription here.
     * Optional — some consultations may have no medicines.
     */
    private String prescription;

    /*
     * Additional clinical notes from doctor.
     * Example: Patient should rest and avoid cold food.
     * Optional — doctor adds any extra observations.
     */
    private String notes;

    /*
     * URL of any attached document.
     * Example: Lab report URL, X-ray image URL from S3
     * Optional — not every consultation has attachments.
     * AWS S3 URL will be stored here in production.
     */
    private String attachmentUrl;

    /*
     * When should patient come back?
     * Example: 2026-04-25
     * Optional — not every consultation needs follow up.
     * When set → NotificationService sends reminder
     * to patient on this date automatically.
     * This is a PDF requirement.
     */
    private LocalDate followUpDate;
}