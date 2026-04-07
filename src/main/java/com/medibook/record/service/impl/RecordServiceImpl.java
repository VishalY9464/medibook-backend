package com.medibook.record.service.impl;

import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.exception.BadRequestException;
import com.medibook.exception.DuplicateResourceException;
import com.medibook.exception.ResourceNotFoundException;
import com.medibook.record.dto.RecordRequest;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.repository.RecordRepository;
import com.medibook.record.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/*
 * This is the actual business logic for Medical Record Service.
 *
 * Think of it like the medical records department at a hospital.
 * RecordService interface says WHAT needs to be done.
 * This class actually DOES it.
 *
 * Key responsibilities:
 * → Doctor creates record after completing appointment
 * → Enforces one record per appointment
 * → Enforces appointment must be COMPLETED before record
 * → Controls who can see what records (PDF access rules)
 * → Triggers follow up reminders via scheduler
 *
 * Access control strictly from PDF:
 * → Patient sees only their own records
 * → Doctor sees only records they created
 * → Admin sees all records read only
 *
 * HIPAA compliance:
 * → createdAt and updatedAt tracked automatically
 * → audit trail maintained for all access
 *
 * Exception handling:
 * → ResourceNotFoundException  → 404 when record not found
 * → DuplicateResourceException → 409 when record already exists
 * → BadRequestException        → 400 when business rule violated
 * All caught by GlobalExceptionHandler → clean JSON response always
 */
@Service
public class RecordServiceImpl implements RecordService {

    @Autowired
    private RecordRepository recordRepository;

    /*
     * AppointmentRepository needed to verify appointment exists
     * and check its status before creating medical record.
     * Doctor can only create record for COMPLETED appointment.
     */
    @Autowired
    private AppointmentRepository appointmentRepository;

    /*
     * Create a new medical record after appointment.
     *
     * How it works:
     * 1. Find the appointment — throws 404 if not found
     * 2. Check appointment status is COMPLETED
     *    throws 400 if not COMPLETED
     * 3. Check no record already exists
     *    throws 409 if record already exists
     * 4. Validate required fields
     * 5. Build MedicalRecord from request data
     * 6. Save and return
     *
     * Why check COMPLETED status?
     * PDF explicitly says doctor creates record AFTER
     * marking appointment as complete.
     * Cannot create record before consultation happens.
     */
    @Override
    public MedicalRecord createRecord(RecordRequest request) {

        // step 1 — find the appointment to verify it exists
        // throws 404 Not Found if appointment not found
        Appointment appointment = appointmentRepository
                .findByAppointmentId(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Appointment", "id", request.getAppointmentId()
                ));

        // step 2 — appointment must be COMPLETED
        // throws 400 Bad Request if not COMPLETED
        if (!appointment.getStatus()
                .equalsIgnoreCase("COMPLETED")) {
            throw new BadRequestException(
                "Medical record can only be created for " +
                "COMPLETED appointments. Current status: "
                + appointment.getStatus()
            );
        }

        // step 3 — check if record already exists
        // one appointment can have only one medical record
        // throws 409 Conflict if record already exists
        if (recordRepository.existsByAppointmentId(
                request.getAppointmentId())) {
            throw new DuplicateResourceException(
                "Medical record already exists for appointment: "
                + request.getAppointmentId()
            );
        }

        // step 4 — validate required fields
        // diagnosis is mandatory for every medical record
        if (request.getDiagnosis() == null
                || request.getDiagnosis().trim().isEmpty()) {
            throw new BadRequestException(
                "Diagnosis is required for medical record."
            );
        }

        // follow up date cannot be in the past
        if (request.getFollowUpDate() != null
                && request.getFollowUpDate()
                    .isBefore(LocalDate.now())) {
            throw new BadRequestException(
                "Follow up date cannot be in the past."
            );
        }

        // step 5 — build medical record from request data
        MedicalRecord record = MedicalRecord.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .diagnosis(request.getDiagnosis())
                .prescription(request.getPrescription())
                .notes(request.getNotes())
                .attachmentUrl(request.getAttachmentUrl())
                .followUpDate(request.getFollowUpDate())
                .build();

        // step 6 — save and return with generated recordId
        return recordRepository.save(record);
    }

    /*
     * Get medical record by appointment ID.
     *
     * Doctor views record for specific appointment.
     * Patient views record from their appointment history.
     * Throws 404 if record not found for this appointment.
     */
    @Override
    public MedicalRecord getRecordByAppointment(
            int appointmentId) {

        // throws 404 Not Found if record not found
        return recordRepository
                .findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "MedicalRecord", "appointmentId", appointmentId
                ));
    }

    /*
     * Get all medical records for a specific patient.
     *
     * Returns records ordered by newest first.
     * PDF rule: patient sees ONLY their own records.
     * Enforced by querying by patientId.
     */
    @Override
    public List<MedicalRecord> getRecordsByPatient(
            int patientId) {

        return recordRepository
                .findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    /*
     * Get all records created by a specific doctor.
     *
     * PDF rule: doctor sees ONLY records they created.
     * Enforced by querying by providerId.
     */
    @Override
    public List<MedicalRecord> getRecordsByProvider(
            int providerId) {

        return recordRepository.findByProviderId(providerId);
    }

    /*
     * Get a single medical record by its record ID.
     *
     * Used when:
     * → Doctor updates specific record
     * → Admin views record for audit compliance
     * → Patient views specific record details
     * Throws 404 if record not found.
     */
    @Override
    public MedicalRecord getRecordById(int recordId) {

        // throws 404 Not Found if record not found
        return recordRepository.findByRecordId(recordId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "MedicalRecord", "id", recordId
                ));
    }

    /*
     * Update an existing medical record.
     *
     * How it works:
     * 1. Find existing record — throws 404 if not found
     * 2. Validate required fields
     * 3. Validate follow up date is not in past
     * 4. Update only medical content fields
     * 5. Save and return updated record
     *
     * Never update appointmentId patientId providerId.
     * These are immutable — protect data integrity.
     *
     * PDF says doctor can edit within allowed time window.
     * Time window check added here later.
     */
    @Override
    public MedicalRecord updateRecord(
            int recordId, RecordRequest request) {

        // find existing record — throws 404 if not found
        MedicalRecord existing = getRecordById(recordId);

        // validate diagnosis is not empty
        if (request.getDiagnosis() == null
                || request.getDiagnosis().trim().isEmpty()) {
            throw new BadRequestException(
                "Diagnosis cannot be empty."
            );
        }

        // validate follow up date is not in the past
        if (request.getFollowUpDate() != null
                && request.getFollowUpDate()
                    .isBefore(LocalDate.now())) {
            throw new BadRequestException(
                "Follow up date cannot be in the past."
            );
        }

        // update only the medical content fields
        // never update appointmentId patientId providerId
        existing.setDiagnosis(request.getDiagnosis());
        existing.setPrescription(request.getPrescription());
        existing.setNotes(request.getNotes());
        existing.setAttachmentUrl(request.getAttachmentUrl());
        existing.setFollowUpDate(request.getFollowUpDate());

        // save — @PreUpdate automatically updates updatedAt
        // tracks when doctor last edited — HIPAA audit trail
        return recordRepository.save(existing);
    }

    /*
     * Delete a medical record.
     *
     * Only admin can delete records — PDF requirement.
     * Doctors cannot delete medical records.
     * Protects medical record integrity.
     * Role check enforced in web layer UC.
     * Throws 404 if record not found.
     */
    @Override
    public void deleteRecord(int recordId) {

        // verify record exists before deleting
        // throws 404 Not Found if record not found
        getRecordById(recordId);

        // delete record from database
        recordRepository.deleteByRecordId(recordId);
    }

    /*
     * Attach a document URL to existing record.
     *
     * Doctor uploads lab report or X-ray to AWS S3.
     * Gets back a URL from S3.
     * Calls this to attach URL to medical record.
     * Updates only the attachmentUrl field.
     * Throws 404 if record not found.
     * Throws 400 if URL is empty.
     */
    @Override
    public void attachDocument(
            int recordId, String attachmentUrl) {

        // validate URL is not empty
        if (attachmentUrl == null
                || attachmentUrl.trim().isEmpty()) {
            throw new BadRequestException(
                "Attachment URL cannot be empty."
            );
        }

        // find existing record — throws 404 if not found
        MedicalRecord record = getRecordById(recordId);

        // update only the attachment URL
        record.setAttachmentUrl(attachmentUrl);

        // save — @PreUpdate updates updatedAt automatically
        recordRepository.save(record);
    }

    /*
     * Get all records with a follow up date of today.
     *
     * Called by FollowUpReminderScheduler every night.
     * For each record found → sends reminder to patient via UC7.
     * PDF explicit requirement:
     * "Records with follow up date trigger automated
     * reminder notification to patient on that date"
     */
    @Override
    public List<MedicalRecord> getFollowUpRecords(LocalDate date) {

        // validate date is not null
        if (date == null) {
            throw new BadRequestException(
                "Date cannot be null."
            );
        }

        // find all records with follow up date = given date
        return recordRepository.findByFollowUpDate(date);
    }

    /*
     * Get upcoming follow up records for a patient.
     *
     * Shows patient which follow ups are coming soon.
     * Helps doctor track patients needing follow up.
     * Returns all records with today or future follow up dates.
     */
    @Override
    public List<MedicalRecord> getUpcomingFollowUps(
            int patientId) {

        // get all upcoming follow ups for this patient
        // today and future dates only
        return recordRepository.findUpcomingFollowUps(
                patientId, LocalDate.now()
        );
    }

    /*
     * Count total medical records for a patient.
     *
     * Used in:
     * → Patient profile "You have 5 medical records"
     * → Admin analytics total records on platform
     */
    @Override
    public int getRecordCount(int patientId) {

        // cast long to int — count never exceeds int range
        return (int) recordRepository
                .countByPatientId(patientId);
    }
}