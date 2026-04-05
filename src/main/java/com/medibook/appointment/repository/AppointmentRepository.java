package com.medibook.appointment.repository;

import com.medibook.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/*
 * This is the Repository for Appointment entity.
 *
 * Think of it like the booking records desk.
 * We ask questions like:
 * "Show me all appointments for patient Rahul"
 * "Show me all appointments for Dr. Sharma today"
 * "How many appointments has Dr. Sharma had this month?"
 *
 * Spring Data JPA writes the SQL automatically
 * from the method names we define here.
 */
@Repository
public interface AppointmentRepository
        extends JpaRepository<Appointment, Integer> {

    /*
     * Get all appointments for a specific patient.
     * Patient views their complete appointment history.
     * Includes all statuses — scheduled, completed, cancelled.
     * Shown on patient dashboard as appointment history.
     */
    List<Appointment> findByPatientId(int patientId);

    /*
     * Get all appointments for a specific doctor.
     * Doctor views all their bookings.
     * Used on provider dashboard to see full appointment list.
     * Admin also uses this to monitor doctor workload.
     */
    List<Appointment> findByProviderId(int providerId);

    /*
     * Find appointment linked to a specific slot.
     * Used when cancelling appointment — we find appointment
     * by slotId then release the slot back to available.
     * Returns Optional because slot might not have appointment.
     */
    Optional<Appointment> findBySlotId(int slotId);

    /*
     * Get all appointments with a specific status.
     * Example: findByStatus("SCHEDULED") → all upcoming
     * Example: findByStatus("COMPLETED") → all done
     * Example: findByStatus("NO_SHOW")   → all no shows
     * Used by admin for platform analytics.
     * Used by scheduler to detect no shows.
     */
    List<Appointment> findByStatus(String status);

    /*
     * Get all appointments for a doctor on a specific date.
     * Doctor views today's schedule or any day's schedule.
     * Example: Dr. Sharma's appointments on April 10.
     * Shown on provider dashboard as today's appointments.
     */
    List<Appointment> findByProviderIdAndAppointmentDate(
            int providerId,
            LocalDate appointmentDate
    );

    /*
     * Get all upcoming appointments for a patient.
     * Upcoming means status=SCHEDULED AND date is today or future.
     * Shown on patient dashboard as "Your Upcoming Appointments".
     * Patient gets reminders for these appointments.
     *
     * We use @Query because we need to check both
     * status AND date together — method name would be too long.
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.patientId = :patientId " +
           "AND a.status = 'SCHEDULED' " +
           "AND a.appointmentDate >= :today")
    List<Appointment> findUpcomingByPatientId(
            @Param("patientId") int patientId,
            @Param("today") LocalDate today
    );

    /*
     * Count total appointments for a doctor.
     * Used in doctor earnings dashboard.
     * Example: "You have handled 150 appointments total"
     * Also used in admin platform analytics.
     */
    long countByProviderId(int providerId);

    /*
     * Find appointment by its ID.
     * Used when updating status, viewing details,
     * or linking to payment and medical record.
     * Returns Optional because appointment might not exist.
     */
    Optional<Appointment> findByAppointmentId(int appointmentId);

    /*
     * Get all appointments for a doctor with specific status.
     * Example: all SCHEDULED appointments for Dr. Sharma
     * Used when doctor views pending appointments only.
     * Also used by no show scheduler to find
     * appointments that passed but were not completed.
     */
    List<Appointment> findByProviderIdAndStatus(
            int providerId,
            String status
    );

    /*
     * Get all appointments that are scheduled but
     * their date has already passed.
     * Used by NoShowDetectionScheduler to auto flag
     * appointments where nobody showed up.
     * Runs automatically every night.
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.status = 'SCHEDULED' " +
           "AND a.appointmentDate < :today")
    List<Appointment> findNoShowAppointments(
            @Param("today") LocalDate today
    );
}