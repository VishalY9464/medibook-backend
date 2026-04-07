package com.medibook.record.repository;

import com.medibook.record.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/*
 * This is the Repository for MedicalRecord entity.
 *
 * Think of it like the medical records room at a hospital.
 * We ask the records room questions like:
 * "Show me all records for patient Rahul"
 * "Show me all records created by Dr. Sharma"
 * "Which patients have follow up appointments today?"
 *
 * Spring Data JPA writes the SQL automatically
 * from the method names we define here.
 * We never write raw SQL manually.
 */
@Repository
public interface RecordRepository
        extends JpaRepository<MedicalRecord, Integer> {

    /*
     * Find medical record by appointment ID.
     * Used when doctor wants to view record for
     * a specific appointment they just completed.
     * Returns Optional because record may not exist yet
     * if doctor has not created it after appointment.
     */
    Optional<MedicalRecord> findByAppointmentId(int appointmentId);

    /*
     * Get all medical records for a specific patient.
     * Patient views their complete medical history.
     * Returns records in any order.
     * Used on patient dashboard medical records tab.
     * PDF rule: patient sees ONLY their own records.
     */
    List<MedicalRecord> findByPatientId(int patientId);

    /*
     * Get all medical records created by a specific doctor.
     * Doctor views all records they have created.
     * Used on provider dashboard medical records section.
     * PDF rule: doctor sees ONLY records they created.
     */
    List<MedicalRecord> findByProviderId(int providerId);

    /*
     * Get all records for a patient ordered by newest first.
     * Used on patient dashboard to show latest records first.
     * Most recent consultation appears at the top.
     * Better user experience than random order.
     */
    List<MedicalRecord> findByPatientIdOrderByCreatedAtDesc(
            int patientId
    );

    /*
     * Find all records that have a specific follow up date.
     * Used by the follow up reminder scheduler.
     * Every night scheduler runs and finds records
     * where followUpDate equals today.
     * For each found record → sends reminder notification
     * to patient via NotificationService (UC7).
     * This is explicit PDF requirement.
     */
    List<MedicalRecord> findByFollowUpDate(LocalDate followUpDate);

    /*
     * Count how many medical records a patient has.
     * Used in patient profile to show
     * "You have 5 medical records on MediBook"
     * Also used in admin analytics.
     */
    long countByPatientId(int patientId);

    /*
     * Delete a specific medical record by its ID.
     * Used by admin for compliance management.
     * Doctors cannot delete records — only admin can.
     * This protects medical record integrity.
     */
    void deleteByRecordId(int recordId);

    /*
     * Find record by its own record ID.
     * Used when updating or viewing specific record.
     * Returns Optional because record might not exist.
     */
    Optional<MedicalRecord> findByRecordId(int recordId);

    /*
     * Find all records that have a follow up date
     * between today and a future date.
     * Used to find upcoming follow ups for a patient.
     * Helps doctor see which patients need follow up soon.
     * Also used in admin health analytics.
     */
    @Query("SELECT r FROM MedicalRecord r WHERE " +
           "r.patientId = :patientId " +
           "AND r.followUpDate IS NOT NULL " +
           "AND r.followUpDate >= :today")
    List<MedicalRecord> findUpcomingFollowUps(
            @Param("patientId") int patientId,
            @Param("today") LocalDate today
    );

    /*
     * Check if a medical record already exists
     * for a specific appointment.
     * Used before creating new record to prevent
     * doctor from creating duplicate records
     * for the same appointment.
     * One appointment can have only one record.
     */
    boolean existsByAppointmentId(int appointmentId);
}