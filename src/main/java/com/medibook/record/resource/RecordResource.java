package com.medibook.record.resource;

import com.medibook.record.dto.RecordRequest;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.service.RecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/*
 * REST Controller for Medical Records.
 * Thin controller — all logic in RecordServiceImpl.
 *
 * Access rules from PDF:
 * Patient → views only their own records
 * Doctor  → views only records they created
 * Admin   → read only access to all records
 */
@RestController
@RequestMapping("/records")
public class RecordResource {

    @Autowired
    private RecordService recordService;

    /*
     * Doctor creates medical record after completing appointment.
     *
     * Who calls: Doctor after marking appointment COMPLETED
     * When: Doctor clicks "Create Medical Record" on dashboard
     * Flow: Appointment COMPLETED → Doctor fills diagnosis
     *       prescription notes → clicks Save Record
     *
     * POST /records/create
     */
    @PostMapping("/create")
    public ResponseEntity<MedicalRecord> createRecord(
            @Valid @RequestBody RecordRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(recordService.createRecord(request));
    }

    /*
     * Get medical record by appointment ID.
     *
     * Who calls: Doctor or Patient
     * When: Viewing record linked to specific appointment
     * Flow: Patient opens appointment → clicks View Record
     *       Doctor opens appointment → views their notes
     *
     * GET /records/appointment/{appointmentId}
     */
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<MedicalRecord> getByAppointment(
            @PathVariable int appointmentId) {

        return ResponseEntity.ok(
                recordService.getRecordByAppointment(appointmentId)
        );
    }

    /*
     * Get all medical records for a patient.
     *
     * Who calls: Patient
     * When: Patient opens Medical Records tab on dashboard
     * Flow: Patient dashboard → Medical Records → all records
     *       PDF rule: patient sees ONLY their own records
     *
     * GET /records/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecord>> getByPatient(
            @PathVariable int patientId) {

        return ResponseEntity.ok(
                recordService.getRecordsByPatient(patientId)
        );
    }

    /*
     * Get all records created by a doctor.
     *
     * Who calls: Doctor
     * When: Doctor opens Medical Records section on dashboard
     * Flow: Provider dashboard → My Records → all records created
     *       PDF rule: doctor sees ONLY records they created
     *
     * GET /records/provider/{providerId}
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<MedicalRecord>> getByProvider(
            @PathVariable int providerId) {

        return ResponseEntity.ok(
                recordService.getRecordsByProvider(providerId)
        );
    }

    /*
     * Get a single record by its ID.
     *
     * Who calls: Doctor / Admin
     * When: Viewing or updating specific record
     * Flow: Admin audit access / Doctor updating their record
     *
     * GET /records/{recordId}
     */
    @GetMapping("/{recordId}")
    public ResponseEntity<MedicalRecord> getById(
            @PathVariable int recordId) {

        return ResponseEntity.ok(
                recordService.getRecordById(recordId)
        );
    }

    /*
     * Doctor updates an existing medical record.
     *
     * Who calls: Doctor
     * When: Doctor edits record within allowed time window
     * Flow: Doctor opens record → edits diagnosis/prescription
     *       → clicks Save Changes
     *
     * PUT /records/{recordId}
     */
    @PutMapping("/{recordId}")
    public ResponseEntity<MedicalRecord> updateRecord(
            @PathVariable int recordId,
            @Valid @RequestBody RecordRequest request) {

        return ResponseEntity.ok(
                recordService.updateRecord(recordId, request)
        );
    }

    /*
     * Admin deletes a medical record.
     *
     * Who calls: Admin only
     * When: Compliance management or incorrect record
     * Flow: Admin → Medical Records → Delete
     *       Doctors cannot delete — only admin can
     *
     * DELETE /records/{recordId}
     */
    @DeleteMapping("/{recordId}")
    public ResponseEntity<?> deleteRecord(
            @PathVariable int recordId) {

        recordService.deleteRecord(recordId);
        return ResponseEntity.ok(Map.of(
                "message", "Medical record deleted successfully."
        ));
    }

    /*
     * Attach a document URL to existing record.
     *
     * Who calls: Doctor
     * When: Doctor uploads lab report or X-ray to S3
     *       then attaches the URL to the medical record
     * Flow: Doctor uploads file → gets S3 URL back
     *       → calls this endpoint to attach URL to record
     *
     * PUT /records/{recordId}/attach?url=https://s3...
     */
    @PutMapping("/{recordId}/attach")
    public ResponseEntity<?> attachDocument(
            @PathVariable int recordId,
            @RequestParam String url) {

        recordService.attachDocument(recordId, url);
        return ResponseEntity.ok(Map.of(
                "message", "Document attached successfully.",
                "attachmentUrl", url
        ));
    }

    /*
     * Get upcoming follow up records for a patient.
     *
     * Who calls: Patient / Doctor
     * When: Patient checks upcoming follow ups on dashboard
     * Flow: Patient dashboard → Upcoming Follow Ups section
     *
     * GET /records/patient/{patientId}/followups
     */
    @GetMapping("/patient/{patientId}/followups")
    public ResponseEntity<List<MedicalRecord>> getUpcomingFollowUps(
            @PathVariable int patientId) {

        return ResponseEntity.ok(
                recordService.getUpcomingFollowUps(patientId)
        );
    }

    /*
     * Get all records with follow up date of today.
     *
     * Who calls: System scheduler (not called by user directly)
     * When: Every night at midnight scheduler runs
     * Flow: Scheduler → finds today's follow ups
     *       → sends reminder notification to each patient
     *       This is explicit PDF requirement.
     *
     * GET /records/followups/today
     */
    @GetMapping("/followups/today")
    public ResponseEntity<List<MedicalRecord>> getTodaysFollowUps() {

        return ResponseEntity.ok(
                recordService.getFollowUpRecords(LocalDate.now())
        );
    }

    /*
     * Get total record count for a patient.
     *
     * Who calls: Patient dashboard / Admin analytics
     * When: Profile page loads
     * Flow: Patient profile shows "You have 5 medical records"
     *
     * GET /records/patient/{patientId}/count
     */
    @GetMapping("/patient/{patientId}/count")
    public ResponseEntity<?> getRecordCount(
            @PathVariable int patientId) {

        return ResponseEntity.ok(Map.of(
                "patientId", patientId,
                "totalRecords", recordService.getRecordCount(patientId)
        ));
    }
}