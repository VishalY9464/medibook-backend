package com.medibook.appointment.resource;

import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/*
 * This is the REST Controller for Appointment.
 *
 * Think of it like the booking counter at a hospital.
 * Patient walks up to counter (sends HTTP request).
 * Counter staff (this controller) takes the request.
 * Passes it to the booking department (AppointmentService).
 * Returns the result back to patient.
 *
 * No business logic here — just receive and respond.
 * All logic lives in AppointmentServiceImpl.
 *
 * @RestController → handles REST API requests
 * @RequestMapping → all URLs start with /appointments
 */
@RestController
@RequestMapping("/appointments")
public class AppointmentResource {

    /*
     * Inject AppointmentService.
     * We depend on interface not implementation.
     * This is loose coupling — good design principle.
     */
    @Autowired
    private AppointmentService appointmentService;

    /*
     * Book a new appointment.
     *
     * Who calls this: Patient confirming a booking
     * What happens: Creates appointment + marks slot booked
     * Why POST: We are creating a new resource
     *
     * URL: POST /appointments/book
     * Body: AppointmentRequest
     * Returns: 201 Created with appointment details
     */
    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(
            @Valid @RequestBody AppointmentRequest request) {

        // call service to book appointment
        Appointment appointment =
                appointmentService.bookAppointment(request);

        // return 201 Created with appointment details
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Appointment booked successfully.",
                        "appointmentId", appointment.getAppointmentId(),
                        "status", appointment.getStatus(),
                        "appointmentDate", appointment.getAppointmentDate(),
                        "startTime", appointment.getStartTime(),
                        "modeOfConsultation", appointment.getModeOfConsultation()
                ));
    }

    /*
     * Get a single appointment by ID.
     *
     * Who calls this: Patient or doctor viewing details
     * What happens: Returns full appointment record
     * Why GET: We are reading a resource
     *
     * URL: GET /appointments/{appointmentId}
     * Returns: 200 OK with appointment details
     */
    @GetMapping("/{appointmentId}")
    public ResponseEntity<Appointment> getById(
            @PathVariable int appointmentId) {

        // call service to get appointment by id
        return ResponseEntity.ok(
                appointmentService.getById(appointmentId)
        );
    }

    /*
     * Get all appointments for a patient.
     *
     * Who calls this: Patient viewing their history
     * What happens: Returns all appointments for this patient
     * Why GET: We are reading resources
     *
     * URL: GET /appointments/patient/{patientId}
     * Returns: 200 OK with list of appointments
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getByPatient(
            @PathVariable int patientId) {

        // call service to get all appointments for patient
        return ResponseEntity.ok(
                appointmentService.getByPatient(patientId)
        );
    }

    /*
     * Get all upcoming appointments for a patient.
     *
     * Who calls this: Patient dashboard loading
     * What happens: Returns only future scheduled appointments
     * Why GET: We are reading resources
     *
     * URL: GET /appointments/patient/{patientId}/upcoming
     * Returns: 200 OK with upcoming appointments only
     */
    @GetMapping("/patient/{patientId}/upcoming")
    public ResponseEntity<List<Appointment>> getUpcoming(
            @PathVariable int patientId) {

        // call service to get upcoming appointments
        return ResponseEntity.ok(
                appointmentService.getUpcomingByPatient(patientId)
        );
    }

    /*
     * Get all appointments for a doctor.
     *
     * Who calls this: Doctor viewing their schedule
     * What happens: Returns all appointments for this doctor
     * Why GET: We are reading resources
     *
     * URL: GET /appointments/provider/{providerId}
     * Returns: 200 OK with list of appointments
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<Appointment>> getByProvider(
            @PathVariable int providerId) {

        // call service to get all appointments for doctor
        return ResponseEntity.ok(
                appointmentService.getByProvider(providerId)
        );
    }

    /*
     * Get all appointments for a doctor on a specific date.
     *
     * Who calls this: Doctor viewing today's schedule
     * What happens: Returns appointments for that date only
     * Why GET: We are reading resources
     *
     * URL: GET /appointments/provider/{providerId}/date?date=2026-04-10
     * Returns: 200 OK with appointments for that date
     */
    @GetMapping("/provider/{providerId}/date")
    public ResponseEntity<List<Appointment>> getByProviderAndDate(
            @PathVariable int providerId,
            @RequestParam String date) {

        // parse date string to LocalDate
        LocalDate appointmentDate = LocalDate.parse(date);

        // call service to get appointments for this date
        return ResponseEntity.ok(
                appointmentService.getByProviderAndDate(
                        providerId,
                        appointmentDate
                )
        );
    }

    /*
     * Cancel an appointment.
     *
     * Who calls this: Patient or doctor cancelling
     * What happens: Status → CANCELLED, slot released
     * Why PUT: We are updating an existing resource
     *
     * URL: PUT /appointments/{appointmentId}/cancel
     * Returns: 200 OK with success message
     */
    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable int appointmentId) {

        // call service to cancel appointment
        appointmentService.cancelAppointment(appointmentId);

        // return success message
        return ResponseEntity.ok(Map.of(
                "message", "Appointment cancelled successfully. " +
                           "Slot has been released for other patients."
        ));
    }

    /*
     * Reschedule an appointment to a new slot.
     *
     * Who calls this: Patient moving to different time
     * What happens: Old slot released, new slot booked
     * Why PUT: We are updating an existing resource
     *
     * URL: PUT /appointments/{appointmentId}/reschedule
     * Body: newSlotId, newDate, newStartTime, newEndTime
     * Returns: 200 OK with updated appointment
     */
    @PutMapping("/{appointmentId}/reschedule")
    public ResponseEntity<Appointment> rescheduleAppointment(
            @PathVariable int appointmentId,
            @RequestBody Map<String, String> body) {

        // extract new slot details from request body
        int newSlotId = Integer.parseInt(body.get("newSlotId"));
        LocalDate newDate = LocalDate.parse(body.get("newDate"));
        String newStartTime = body.get("newStartTime");
        String newEndTime = body.get("newEndTime");

        // call service to reschedule appointment
        Appointment updated = appointmentService.rescheduleAppointment(
                appointmentId,
                newSlotId,
                newDate,
                newStartTime,
                newEndTime
        );

        // return updated appointment
        return ResponseEntity.ok(updated);
    }

    /*
     * Doctor marks appointment as completed.
     *
     * Who calls this: Doctor after consultation is done
     * What happens: Status → COMPLETED
     *               Unlocks review submission for patient
     *               Unlocks medical record creation for doctor
     * Why PUT: We are updating an existing resource
     *
     * URL: PUT /appointments/{appointmentId}/complete
     * Returns: 200 OK with success message
     */
    @PutMapping("/{appointmentId}/complete")
    public ResponseEntity<?> completeAppointment(
            @PathVariable int appointmentId) {

        // call service to mark appointment as completed
        appointmentService.completeAppointment(appointmentId);

        // return success message
        return ResponseEntity.ok(Map.of(
                "message", "Appointment marked as completed. " +
                           "Patient can now submit a review."
        ));
    }

    /*
     * Update appointment status manually.
     *
     * Who calls this: Admin fixing status or scheduler
     * What happens: Status updated to given value
     * Why PUT: We are updating an existing resource
     *
     * URL: PUT /appointments/{appointmentId}/status?status=NO_SHOW
     * Returns: 200 OK with success message
     */
    @PutMapping("/{appointmentId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable int appointmentId,
            @RequestParam String status) {

        // call service to update status
        appointmentService.updateStatus(appointmentId, status);

        // return success message
        return ResponseEntity.ok(Map.of(
                "message", "Appointment status updated to: " + status
        ));
    }

    /*
     * Get total appointment count for a doctor.
     *
     * Who calls this: Doctor dashboard or admin analytics
     * What happens: Returns total count of appointments
     * Why GET: We are reading a value
     *
     * URL: GET /appointments/provider/{providerId}/count
     * Returns: 200 OK with count
     */
    @GetMapping("/provider/{providerId}/count")
    public ResponseEntity<?> getCount(
            @PathVariable int providerId) {

        // call service to get appointment count
        int count = appointmentService.getAppointmentCount(providerId);

        // return count
        return ResponseEntity.ok(Map.of(
                "providerId", providerId,
                "totalAppointments", count
        ));
    }
}