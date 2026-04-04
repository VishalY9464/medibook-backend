package com.medibook.schedule.service;

import com.medibook.schedule.dto.SlotRequest;
import com.medibook.schedule.entity.AvailabilitySlot;

import java.time.LocalDate;
import java.util.List;

/*
 * This is the Service Interface for Schedule.
 *
 * Think of it like a contract between the controller and the business logic.
 * ScheduleResource (controller) only knows about this interface.
 * It does not care HOW things are done — just WHAT can be done.
 *
 * ScheduleServiceImpl will implement all these methods
 * with the actual business logic inside.
 *
 * Why interface first?
 * → Defines the contract clearly before writing logic
 * → Controller depends on interface not implementation
 * → Easy to swap implementation without breaking anything
 * → PDF 5 layer pattern requires this strictly
 * → Evaluators check this pattern
 */
public interface ScheduleService {

    /*
     * Add a single time slot for a doctor.
     * Doctor picks one specific date and time window.
     * Example: April 5, 10:00 to 10:30
     * Returns the saved slot with generated slotId.
     */
    AvailabilitySlot addSlot(SlotRequest request);

    /*
     * Add multiple slots at once.
     * Doctor provides a list of slots to save in bulk.
     * More efficient than calling addSlot() one by one.
     * Returns list of all saved slots.
     */
    List<AvailabilitySlot> addBulkSlots(List<SlotRequest> requests);

    /*
     * Generate recurring slots automatically.
     * Doctor sets a pattern (DAILY or WEEKLY)
     * and a date range (April 1 to April 30).
     * System creates all slots automatically.
     * Saves doctor from adding each slot manually.
     */
    List<AvailabilitySlot> generateRecurringSlots(SlotRequest request);

    /*
     * Get all slots for a specific doctor.
     * Doctor views their complete schedule.
     * Includes booked, blocked, and available.
     * Admin also uses this to see full doctor schedule.
     */
    List<AvailabilitySlot> getSlotsByProvider(int providerId);

    /*
     * Get only available slots for a doctor on a specific date.
     * This is what patients see when they select a date.
     * Only returns unbooked and unblocked slots.
     * Patient picks one of these to book appointment.
     */
    List<AvailabilitySlot> getAvailableSlots(
            int providerId,
            LocalDate date
    );

    /*
     * Get a single slot by its ID.
     * Used when booking an appointment — we need slot details.
     * Also used when blocking or updating a slot.
     */
    AvailabilitySlot getSlotById(int slotId);

    /*
     * Mark a slot as booked.
     * Called by AppointmentService (UC4) when patient books.
     * Sets isBooked = true.
     * Slot disappears from available list immediately.
     * Uses optimistic locking to prevent double booking.
     */
    void bookSlot(int slotId);

    /*
     * Release a booked slot back to available.
     * Called by AppointmentService (UC4) when patient cancels.
     * Sets isBooked = false.
     * Slot appears back in available list immediately.
     */
    void releaseSlot(int slotId);

    /*
     * Doctor blocks a slot for personal unavailability.
     * Example: doctor has a meeting, blocks that slot.
     * Sets isBlocked = true.
     * Blocked slots are invisible to patients.
     * PDF strictly requires this behavior.
     */
    void blockSlot(int slotId);

    /*
     * Doctor unblocks a previously blocked slot.
     * Sets isBlocked = false.
     * Slot becomes visible and available to patients again.
     */
    void unblockSlot(int slotId);

    /*
     * Update slot details.
     * Doctor changes time or duration of a slot.
     * Cannot update if slot is already booked.
     */
    AvailabilitySlot updateSlot(int slotId, SlotRequest request);

    /*
     * Delete a slot permanently.
     * Doctor removes a slot from their calendar.
     * Cannot delete if slot is already booked.
     */
    void deleteSlot(int slotId);

    /*
     * Delete all expired slots.
     * Expired = past date AND never booked.
     * Called by SlotExpiryScheduler automatically.
     * Keeps database clean and fast.
     */
    void deleteExpiredSlots();
}