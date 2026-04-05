package com.medibook.appointment.service;

import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

/*
 * This is the Service Interface for Appointment.
 *
 * Think of it like the rules book for appointment management.
 * It defines WHAT operations are possible.
 * AppointmentServiceImpl defines HOW each operation works.
 *
 * This is the most important service in the entire system.
 * It orchestrates everything:
 * → Calls ScheduleService to book/release slots (UC3)
 * → Will trigger PaymentService for refunds (UC5)
 * → Will trigger NotificationService for alerts (UC7)
 *
 * Why interface first?
 * → Defines contract before writing logic
 * → AppointmentResource depends on this interface
 * → Easy to change implementation without breaking anything
 * → PDF 5 layer pattern requires this strictly
 */
public interface AppointmentService {

    /*
     * Book a new appointment.
     * Patient selects doctor, slot, date, mode.
     * System creates appointment record.
     * System calls ScheduleService to mark slot as booked.
     * Returns saved appointment with generated appointmentId.
     */
    Appointment bookAppointment(AppointmentRequest request);

    /*
     * Get a single appointment by its ID.
     * Used when viewing appointment details.
     * Also used by Payment and MedicalRecord services
     * to verify appointment exists before linking.
     */
    Appointment getById(int appointmentId);

    /*
     * Get all appointments for a specific patient.
     * Patient views their complete appointment history.
     * Includes all statuses — scheduled, completed, cancelled.
     */
    List<Appointment> getByPatient(int patientId);

    /*
     * Get all appointments for a specific doctor.
     * Doctor views all their bookings on dashboard.
     * Admin monitors doctor workload.
     */
    List<Appointment> getByProvider(int providerId);

    /*
     * Get all appointments for a doctor on a specific date.
     * Doctor views today's schedule.
     * Example: who is coming in today April 10?
     */
    List<Appointment> getByProviderAndDate(
            int providerId,
            LocalDate date
    );

    /*
     * Get all upcoming appointments for a patient.
     * Upcoming = status SCHEDULED AND date is today or future.
     * Shown on patient dashboard as upcoming appointments.
     * Notification reminders sent for these.
     */
    List<Appointment> getUpcomingByPatient(int patientId);

    /*
     * Cancel an appointment.
     * Patient or doctor can cancel.
     * Status changes to CANCELLED.
     * Slot is released back to available.
     * Refund triggered if payment was made (UC5).
     * Notification sent to both patient and doctor (UC7).
     */
    void cancelAppointment(int appointmentId);

    /*
     * Reschedule an appointment to a different slot.
     * Patient moves booking to another available slot
     * with the same doctor.
     * Old slot is released.
     * New slot is booked.
     * Status stays SCHEDULED.
     * Notifications sent (UC7).
     */
    Appointment rescheduleAppointment(
            int appointmentId,
            int newSlotId,
            LocalDate newDate,
            String newStartTime,
            String newEndTime
    );

    /*
     * Doctor marks appointment as completed.
     * Status changes to COMPLETED.
     * Patient can now submit a review (UC6).
     * Doctor can now create medical record (UC8).
     * Notification sent to patient (UC7).
     */
    void completeAppointment(int appointmentId);

    /*
     * Update appointment status manually.
     * Used by admin to fix incorrect statuses.
     * Also used by NoShowDetectionScheduler
     * to auto set status to NO_SHOW.
     */
    void updateStatus(int appointmentId, String status);

    /*
     * Count total appointments for a doctor.
     * Used in doctor earnings dashboard.
     * Also used in admin analytics.
     */
    int getAppointmentCount(int providerId);
}