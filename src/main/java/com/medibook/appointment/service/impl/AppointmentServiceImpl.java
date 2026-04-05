package com.medibook.appointment.service.impl;

import com.medibook.appointment.dto.AppointmentRequest;
import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.service.AppointmentService;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/*
 * This is the actual business logic for Appointment Service.
 *
 * This is the most important service in the entire MediBook system.
 * Think of it like the booking desk at a hospital.
 * It takes a patient request, verifies everything is correct,
 * books the slot, creates the appointment record,
 * and manages the entire lifecycle from booking to completion.
 *
 * Key connections to other services:
 * → ScheduleService (UC3) → to book and release slots
 * → PaymentService (UC5)  → will be added when UC5 is built
 * → NotificationService   → will be added when UC7 is built
 *
 * @Transactional on bookAppointment and cancelAppointment
 * is critical — if anything fails midway,
 * database rolls back to original state.
 * No half completed bookings ever.
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    /*
     * AppointmentRepository — our connection to appointments table.
     * Spring injects this automatically.
     */
    @Autowired
    private AppointmentRepository appointmentRepository;

    /*
     * ScheduleService — we call this to book and release slots.
     * This is how UC4 talks to UC3.
     * When patient books → we call scheduleService.bookSlot()
     * When patient cancels → we call scheduleService.releaseSlot()
     */
    @Autowired
    private ScheduleService scheduleService;

    /*
     * Book a new appointment.
     *
     * How it works step by step:
     * 1. Get the slot details from UC3
     * 2. Verify slot is available (not booked, not blocked)
     * 3. Verify slot belongs to correct provider
     * 4. Create Appointment record with status=SCHEDULED
     * 5. Save appointment to database
     * 6. Call ScheduleService to mark slot as booked
     *
     * @Transactional means:
     * If saving appointment fails → slot never gets booked
     * If slot booking fails → appointment save rolls back
     * Either both happen or neither happens.
     */
    @Override
    @Transactional
    public Appointment bookAppointment(AppointmentRequest request) {

        // get the slot from UC3 to verify it exists
        AvailabilitySlot slot = scheduleService
                .getSlotById(request.getSlotId());

        // verify slot is not already booked by another patient
        if (slot.isBooked()) {
            throw new RuntimeException(
                "This slot is already booked. Please choose another slot."
            );
        }

        // verify slot is not blocked by the doctor
        if (slot.isBlocked()) {
            throw new RuntimeException(
                "This slot is blocked by the doctor. Please choose another slot."
            );
        }

        // verify slot belongs to the requested provider
        // patient cannot book a slot from a different doctor
        if (slot.getProviderId() != request.getProviderId()) {
            throw new RuntimeException(
                "This slot does not belong to the selected doctor."
            );
        }

        // build the appointment record
        // status is SCHEDULED by default when first created
        Appointment appointment = Appointment.builder()
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .slotId(request.getSlotId())
                .serviceType(request.getServiceType())
                .appointmentDate(request.getAppointmentDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .modeOfConsultation(request.getModeOfConsultation())
                .notes(request.getNotes())
                .status("SCHEDULED")
                .build();

        // save appointment to database
        Appointment saved = appointmentRepository.save(appointment);

        // mark the slot as booked in UC3
        // optimistic locking prevents double booking
        scheduleService.bookSlot(request.getSlotId());

        // return saved appointment with generated appointmentId
        return saved;
    }

    /*
     * Get a single appointment by its ID.
     *
     * Used when viewing appointment details.
     * Also used by Payment and MedicalRecord services
     * to verify appointment exists before linking.
     */
    @Override
    public Appointment getById(int appointmentId) {

        // find appointment — throw exception if not found
        return appointmentRepository
                .findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException(
                    "Appointment not found with id: " + appointmentId
                ));
    }

    /*
     * Get all appointments for a specific patient.
     *
     * Patient views their complete appointment history.
     * Shows all statuses — scheduled, completed, cancelled.
     * Patient uses this to track their medical visits.
     */
    @Override
    public List<Appointment> getByPatient(int patientId) {

        // get all appointments for this patient from database
        return appointmentRepository.findByPatientId(patientId);
    }

    /*
     * Get all appointments for a specific doctor.
     *
     * Doctor views all their bookings on dashboard.
     * Includes past and upcoming appointments.
     * Admin uses this to monitor doctor workload.
     */
    @Override
    public List<Appointment> getByProvider(int providerId) {

        // get all appointments for this doctor from database
        return appointmentRepository.findByProviderId(providerId);
    }

    /*
     * Get all appointments for a doctor on a specific date.
     *
     * Doctor views their schedule for a specific day.
     * Most commonly used for today's appointments.
     * Shown on provider dashboard as daily schedule.
     */
    @Override
    public List<Appointment> getByProviderAndDate(
            int providerId,
            LocalDate date) {

        // get all appointments for this doctor on this specific date
        return appointmentRepository
                .findByProviderIdAndAppointmentDate(providerId, date);
    }

    /*
     * Get all upcoming appointments for a patient.
     *
     * Upcoming means:
     * → status = SCHEDULED
     * → date is today or in the future
     *
     * Shown on patient dashboard as upcoming appointments.
     * Notification reminders are sent for these.
     */
    @Override
    public List<Appointment> getUpcomingByPatient(int patientId) {

        // get only future scheduled appointments for this patient
        return appointmentRepository.findUpcomingByPatientId(
                patientId,
                LocalDate.now()
        );
    }

    /*
     * Cancel an appointment.
     *
     * How it works:
     * 1. Find the appointment
     * 2. Check it is SCHEDULED (cannot cancel completed)
     * 3. Change status to CANCELLED
     * 4. Save updated appointment
     * 5. Release the slot back to available in UC3
     *
     * @Transactional ensures both steps happen together.
     * If slot release fails → appointment stays SCHEDULED.
     * No inconsistent state in database ever.
     */
    @Override
    @Transactional
    public void cancelAppointment(int appointmentId) {

        // find the appointment
        Appointment appointment = getById(appointmentId);

        // can only cancel scheduled appointments
        // cannot cancel an already completed appointment
        if (!appointment.getStatus().equals("SCHEDULED")) {
            throw new RuntimeException(
                "Only scheduled appointments can be cancelled. " +
                "Current status: " + appointment.getStatus()
            );
        }

        // change status to CANCELLED
        appointment.setStatus("CANCELLED");

        // save updated appointment
        // @PreUpdate automatically updates updatedAt timestamp
        appointmentRepository.save(appointment);

        // release the slot back to available in UC3
        // slot immediately becomes bookable by other patients
        scheduleService.releaseSlot(appointment.getSlotId());

        // TODO: trigger refund if payment was made (UC5)
        // TODO: send cancellation notification to patient and doctor (UC7)
    }

    /*
     * Reschedule an appointment to a different slot.
     *
     * How it works:
     * 1. Find the existing appointment
     * 2. Verify it is SCHEDULED
     * 3. Verify new slot belongs to same doctor
     * 4. Release the old slot back to available
     * 5. Book the new slot
     * 6. Update appointment with new slot details
     * 7. Save and return updated appointment
     *
     * PDF rule: rescheduling only allowed with same provider.
     * Patient cannot switch doctor by rescheduling.
     */
    @Override
    @Transactional
    public Appointment rescheduleAppointment(
            int appointmentId,
            int newSlotId,
            LocalDate newDate,
            String newStartTime,
            String newEndTime) {

        // find existing appointment
        Appointment appointment = getById(appointmentId);

        // can only reschedule scheduled appointments
        if (!appointment.getStatus().equals("SCHEDULED")) {
            throw new RuntimeException(
                "Only scheduled appointments can be rescheduled. " +
                "Current status: " + appointment.getStatus()
            );
        }

        // get new slot to verify it exists and is available
        AvailabilitySlot newSlot = scheduleService.getSlotById(newSlotId);

        // new slot must belong to same doctor
        // patient cannot switch doctor by rescheduling
        if (newSlot.getProviderId() != appointment.getProviderId()) {
            throw new RuntimeException(
                "Rescheduling is only allowed with the same doctor. " +
                "Please book a new appointment for a different doctor."
            );
        }

        // release the old slot back to available
        scheduleService.releaseSlot(appointment.getSlotId());

        // book the new slot
        scheduleService.bookSlot(newSlotId);

        // update appointment with new slot details
        appointment.setSlotId(newSlotId);
        appointment.setAppointmentDate(newDate);
        appointment.setStartTime(LocalTime.parse(newStartTime));
        appointment.setEndTime(LocalTime.parse(newEndTime));

        // save and return updated appointment
        // @PreUpdate automatically updates updatedAt timestamp
        return appointmentRepository.save(appointment);

        // TODO: send rescheduling notification (UC7)
    }

    /*
     * Doctor marks appointment as completed.
     *
     * How it works:
     * 1. Find the appointment
     * 2. Verify it is SCHEDULED
     * 3. Change status to COMPLETED
     * 4. Save updated appointment
     *
     * After this:
     * → Patient can now submit a review (UC6)
     * → Doctor can now create medical record (UC8)
     * → Both are unlocked only after COMPLETED status
     */
    @Override
    public void completeAppointment(int appointmentId) {

        // find the appointment
        Appointment appointment = getById(appointmentId);

        // can only complete a scheduled appointment
        if (!appointment.getStatus().equals("SCHEDULED")) {
            throw new RuntimeException(
                "Only scheduled appointments can be marked as completed. " +
                "Current status: " + appointment.getStatus()
            );
        }

        // change status to COMPLETED
        appointment.setStatus("COMPLETED");

        // save updated appointment
        // @PreUpdate auto updates updatedAt — part of audit trail
        appointmentRepository.save(appointment);

        // TODO: send completion notification to patient (UC7)
    }

    /*
     * Update appointment status manually.
     *
     * Used by:
     * → Admin to fix incorrect statuses
     * → NoShowDetectionScheduler to auto set NO_SHOW
     *   for past SCHEDULED appointments nobody completed
     *
     * Valid statuses from PDF:
     * SCHEDULED / COMPLETED / CANCELLED / NO_SHOW
     */
    @Override
    public void updateStatus(int appointmentId, String status) {

        // find the appointment
        Appointment appointment = getById(appointmentId);

        // update to new status
        appointment.setStatus(status);

        // save — @PreUpdate auto updates updatedAt timestamp
        // all status changes are tracked — PDF audit trail requirement
        appointmentRepository.save(appointment);
    }

    /*
     * Count total appointments for a doctor.
     *
     * Used in:
     * → Doctor earnings dashboard
     *   "You have handled 150 appointments total"
     * → Admin platform analytics
     *   to show most active doctors
     */
    @Override
    public int getAppointmentCount(int providerId) {

        // count all appointments for this doctor
        // cast long to int — count will never exceed int range
        return (int) appointmentRepository.countByProviderId(providerId);
    }
}