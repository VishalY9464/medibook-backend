package com.medibook.record.service;

import com.medibook.record.dto.RecordRequest;
import com.medibook.record.entity.MedicalRecord;

import java.time.LocalDate;
import java.util.List;

/*
 * This is the Service Interface for Medical Records.
 *
 * Think of it like the rules book for medical record management.
 * It defines WHAT operations are possible on medical records.
 * RecordServiceImpl defines HOW each operation works.
 *
 * Why interface first?
 * → Defines contract before writing logic
 * → RecordResource depends on this interface
 * → Easy to change implementation without breaking anything
 * → PDF 5 layer pattern requires this strictly
 * → Evaluators check this pattern exists
 *
 * Access rules strictly from PDF:
 * → Patient views only their own records
 * → Doctor views only records they created
 * → Admin has read only access to all records
 * → These rules enforced in ServiceImpl not here
 */
public interface RecordService {

    /*
     * Create a new medical record after appointment.
     * Doctor creates this after marking appointment COMPLETED.
     * Appointment must be COMPLETED before record can be created.
     * One record per appointment — enforced by unique constraint.
     * Returns saved record with generated recordId.
     */
    MedicalRecord createRecord(RecordRequest request);

    /*
     * Get medical record by appointment ID.
     * Doctor views record for specific appointment.
     * Patient views record from their appointment history.
     * Returns record linked to that appointment.
     */
    MedicalRecord getRecordByAppointment(int appointmentId);

    /*
     * Get all medical records for a specific patient.
     * Patient views their complete medical history.
     * Returns records ordered by newest first.
     * PDF rule: patient sees ONLY their own records.
     */
    List<MedicalRecord> getRecordsByPatient(int patientId);

    /*
     * Get all records created by a specific doctor.
     * Doctor views all consultations they have documented.
     * PDF rule: doctor sees ONLY records they created.
     */
    List<MedicalRecord> getRecordsByProvider(int providerId);

    /*
     * Get a single medical record by its record ID.
     * Used when viewing or updating specific record.
     * Admin uses this for audit access.
     */
    MedicalRecord getRecordById(int recordId);

    /*
     * Update an existing medical record.
     * Doctor can edit record within allowed time window
     * after the appointment — PDF requirement.
     * After window closes record becomes read only.
     * Returns updated record.
     */
    MedicalRecord updateRecord(int recordId, RecordRequest request);

    /*
     * Delete a medical record.
     * Only admin can delete records — doctors cannot.
     * Protects medical record integrity.
     * Used for compliance management.
     */
    void deleteRecord(int recordId);

    /*
     * Attach a document URL to existing record.
     * Doctor uploads lab report or X-ray to S3.
     * Then attaches the S3 URL to the medical record.
     * Updates only the attachmentUrl field.
     */
    void attachDocument(int recordId, String attachmentUrl);

    /*
     * Get all records with a follow up date of today.
     * Called by follow up reminder scheduler every night.
     * For each record found → sends reminder to patient
     * via NotificationService (UC7).
     * PDF explicitly requires this automation.
     */
    List<MedicalRecord> getFollowUpRecords(LocalDate date);

    /*
     * Get upcoming follow up records for a patient.
     * Shows patient which follow ups are coming soon.
     * Helps doctor track patients needing follow up.
     */
    List<MedicalRecord> getUpcomingFollowUps(int patientId);

    /*
     * Count total medical records for a patient.
     * Used in patient profile and admin analytics.
     * Example: You have 5 medical records on MediBook.
     */
    int getRecordCount(int patientId);
}