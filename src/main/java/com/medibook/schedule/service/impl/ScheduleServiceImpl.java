package com.medibook.schedule.service.impl;

import com.medibook.exception.BadRequestException;
import com.medibook.exception.ResourceNotFoundException;
import com.medibook.schedule.dto.SlotRequest;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.repository.SlotRepository;
import com.medibook.schedule.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
 * This is the actual business logic for Schedule Service.
 *
 * Think of it like the actual receptionist who manages the doctor's diary.
 * ScheduleService (interface) says WHAT needs to be done.
 * This class actually DOES it — adds slots, blocks them, releases them.
 *
 * Exception handling:
 * → ResourceNotFoundException → 404 when slot not found
 * → BadRequestException       → 400 when business rule violated
 * All caught by GlobalExceptionHandler → clean JSON response always
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private SlotRepository slotRepository;

    /*
     * Add a single time slot for a doctor.
     *
     * How it works:
     * 1. Validate date is not in the past
     * 2. Build AvailabilitySlot object
     * 3. Set defaults — isBooked=false, isBlocked=false
     * 4. Save to database and return
     */
    @Override
    public AvailabilitySlot addSlot(SlotRequest request) {

        // do not allow creating slots in the past
        // throws 400 Bad Request if date is in the past
        if (request.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException(
                "Cannot create slot in the past. " +
                "Please select a future date."
            );
        }

        // build the slot object from request data
        // isBooked and isBlocked are false by default
        // slot is available as soon as it is created
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .providerId(request.getProviderId())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .recurrence(request.getRecurrence())
                .isBooked(false)
                .isBlocked(false)
                .build();

        // save to database and return saved slot
        return slotRepository.save(slot);
    }

    /*
     * Add multiple slots at once.
     *
     * Doctor sends a list of slot requests.
     * We loop through each one and save them all.
     * More efficient than calling addSlot() separately.
     */
    @Override
    public List<AvailabilitySlot> addBulkSlots(
            List<SlotRequest> requests) {

        // validate list is not empty
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException(
                "Slot list cannot be empty."
            );
        }

        // list to collect all saved slots
        List<AvailabilitySlot> savedSlots = new ArrayList<>();

        // loop through each request and save each slot
        // addSlot() validates each slot individually
        for (SlotRequest request : requests) {
            AvailabilitySlot slot = addSlot(request);
            savedSlots.add(slot);
        }

        return savedSlots;
    }

    /*
     * Generate recurring slots automatically.
     *
     * DAILY  → creates one slot every day from start to end date
     * WEEKLY → creates one slot every week on same day of week
     *
     * Example:
     * Doctor sets Monday 10:00-10:30 WEEKLY from April 1 to April 30
     * → System creates: April 7, April 14, April 21, April 28
     */
    @Override
    public List<AvailabilitySlot> generateRecurringSlots(
            SlotRequest request) {

        // recurrence end date is required for recurring slots
        if (request.getRecurrenceEndDate() == null) {
            throw new BadRequestException(
                "Recurrence end date is required for recurring slots."
            );
        }

        // end date must be after start date
        if (request.getRecurrenceEndDate()
                .isBefore(request.getDate())) {
            throw new BadRequestException(
                "Recurrence end date must be after start date."
            );
        }

        // recurrence pattern must be DAILY or WEEKLY
        String recurrence = request.getRecurrence();
        if (recurrence == null
                || (!recurrence.equals("DAILY")
                    && !recurrence.equals("WEEKLY"))) {
            throw new BadRequestException(
                "Recurrence pattern must be DAILY or WEEKLY."
            );
        }

        // list to collect all generated slots
        List<AvailabilitySlot> generatedSlots = new ArrayList<>();

        // start from the given date
        LocalDate currentDate = request.getDate();
        LocalDate endDate = request.getRecurrenceEndDate();

        // keep creating slots until we reach the end date
        while (!currentDate.isAfter(endDate)) {

            // build slot for this date with same time and duration
            AvailabilitySlot slot = AvailabilitySlot.builder()
                    .providerId(request.getProviderId())
                    .date(currentDate)
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .durationMinutes(request.getDurationMinutes())
                    .recurrence(request.getRecurrence())
                    .isBooked(false)
                    .isBlocked(false)
                    .build();

            // save this slot to database
            generatedSlots.add(slotRepository.save(slot));

            // move to next date based on recurrence pattern
            if (recurrence.equals("DAILY")) {
                currentDate = currentDate.plusDays(1);
            } else {
                // WEEKLY → move to same day next week
                currentDate = currentDate.plusWeeks(1);
            }
        }

        return generatedSlots;
    }

    /*
     * Get all slots for a specific doctor.
     * Returns everything — booked, blocked, and available.
     * Doctor needs to see all of them to manage their calendar.
     */
    @Override
    public List<AvailabilitySlot> getSlotsByProvider(int providerId) {
        return slotRepository.findByProviderId(providerId);
    }

    /*
     * Get only available slots for a doctor on a specific date.
     * Only slots that are not booked and not blocked.
     * This is what patients see when they pick a date.
     */
    @Override
    public List<AvailabilitySlot> getAvailableSlots(
            int providerId, LocalDate date) {

        // validate date is not null
        if (date == null) {
            throw new BadRequestException(
                "Date cannot be null."
            );
        }

        return slotRepository.findAvailableByProviderAndDate(
                providerId, date
        );
    }

    /*
     * Get a single slot by its ID.
     * Throws 404 Not Found if slot does not exist.
     * Called internally by bookSlot, blockSlot, deleteSlot etc.
     */
    @Override
    public AvailabilitySlot getSlotById(int slotId) {

        // throws 404 Not Found if slot not found
        return slotRepository.findBySlotId(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Slot", "id", slotId
                ));
    }

    /*
     * Mark a slot as booked.
     * Called by AppointmentService in UC4.
     *
     * @Transactional ensures atomic operation.
     * @Version on entity prevents double booking.
     * If two patients try simultaneously:
     * → First saves → version = 1
     * → Second tries → version mismatch → exception
     * → Only first patient gets the slot
     */
    @Override
    @Transactional
    public void bookSlot(int slotId) {

        // find the slot — throws 404 if not found
        AvailabilitySlot slot = getSlotById(slotId);

        // check if slot is already booked
        // safety check on top of optimistic locking
        if (slot.isBooked()) {
            throw new BadRequestException(
                "This slot is already booked. " +
                "Please choose another slot."
            );
        }

        // check if slot is blocked by doctor
        if (slot.isBlocked()) {
            throw new BadRequestException(
                "This slot is blocked by the doctor " +
                "and cannot be booked."
            );
        }

        // mark slot as booked
        slot.setBooked(true);

        // save — @Version automatically increments
        // concurrent booking attempt → OptimisticLockException
        slotRepository.save(slot);
    }

    /*
     * Release a booked slot back to available.
     * Called by AppointmentService when patient cancels.
     * Slot becomes available immediately for other patients.
     */
    @Override
    @Transactional
    public void releaseSlot(int slotId) {

        // find the slot — throws 404 if not found
        AvailabilitySlot slot = getSlotById(slotId);

        // set isBooked back to false
        slot.setBooked(false);

        // save updated slot
        slotRepository.save(slot);
    }

    /*
     * Doctor blocks a slot for personal unavailability.
     * Blocked slot becomes INVISIBLE to patients.
     * Cannot block already booked slot.
     */
    @Override
    public void blockSlot(int slotId) {

        // find the slot — throws 404 if not found
        AvailabilitySlot slot = getSlotById(slotId);

        // cannot block a slot already booked by patient
        if (slot.isBooked()) {
            throw new BadRequestException(
                "Cannot block a slot that is already " +
                "booked by a patient."
            );
        }

        // check if already blocked
        if (slot.isBlocked()) {
            throw new BadRequestException(
                "Slot is already blocked."
            );
        }

        // set isBlocked to true
        // slot disappears from patient view immediately
        slot.setBlocked(true);

        slotRepository.save(slot);
    }

    /*
     * Doctor unblocks a previously blocked slot.
     * Sets isBlocked back to false.
     * Slot appears in patient search immediately.
     */
    @Override
    public void unblockSlot(int slotId) {

        // find the slot — throws 404 if not found
        AvailabilitySlot slot = getSlotById(slotId);

        // check if already unblocked
        if (!slot.isBlocked()) {
            throw new BadRequestException(
                "Slot is not blocked."
            );
        }

        // set isBlocked back to false
        slot.setBlocked(false);

        slotRepository.save(slot);
    }

    /*
     * Update slot details.
     * Cannot update if patient has already booked it.
     */
    @Override
    public AvailabilitySlot updateSlot(
            int slotId, SlotRequest request) {

        // find existing slot — throws 404 if not found
        AvailabilitySlot existing = getSlotById(slotId);

        // cannot update a slot already booked by patient
        if (existing.isBooked()) {
            throw new BadRequestException(
                "Cannot update a slot that is already " +
                "booked by a patient."
            );
        }

        // validate new date is not in the past
        if (request.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException(
                "Cannot update slot to a past date."
            );
        }

        // update the slot fields with new values
        existing.setDate(request.getDate());
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setDurationMinutes(request.getDurationMinutes());

        return slotRepository.save(existing);
    }

    /*
     * Delete a slot permanently from calendar.
     * Cannot delete if patient has already booked it.
     */
    @Override
    @Transactional
    public void deleteSlot(int slotId) {

        // find the slot — throws 404 if not found
        AvailabilitySlot slot = getSlotById(slotId);

        // cannot delete a slot already booked by patient
        if (slot.isBooked()) {
            throw new BadRequestException(
                "Cannot delete a slot that is already booked " +
                "by a patient. Please cancel the appointment first."
            );
        }

        slotRepository.deleteBySlotId(slotId);
    }

    /*
     * Delete all expired slots automatically.
     * Called by SlotExpiryScheduler every night at midnight.
     * Expired = past date AND never booked.
     */
    @Override
    @Transactional
    public void deleteExpiredSlots() {

        // find all slots where date is before today
        // and nobody booked them
        List<AvailabilitySlot> expiredSlots =
                slotRepository.findExpiredSlots(LocalDate.now());

        // delete each expired slot
        for (AvailabilitySlot slot : expiredSlots) {
            slotRepository.deleteBySlotId(slot.getSlotId());
        }
    }
}