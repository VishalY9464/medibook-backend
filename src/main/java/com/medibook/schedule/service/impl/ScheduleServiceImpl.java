package com.medibook.schedule.service.impl;

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
 * Key things happening here:
 * → Doctor adds time slots to their calendar
 * → Slots can be individual or recurring (daily/weekly)
 * → Patients see only available slots (not booked, not blocked)
 * → When patient books → slot marked as booked
 * → When patient cancels → slot released back
 * → Expired slots cleaned up automatically
 *
 * @Transactional on bookSlot() is critical.
 * It ensures the entire booking operation is atomic.
 * Either the whole thing succeeds or nothing changes.
 * Combined with @Version on entity → prevents double booking.
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    /*
     * SlotRepository is our connection to availability_slots table.
     * Spring injects this automatically.
     * We never create it manually with new keyword.
     */
    @Autowired
    private SlotRepository slotRepository;

    /*
     * Add a single time slot for a doctor.
     *
     * How it works:
     * 1. Take slot details from request
     * 2. Build AvailabilitySlot object
     * 3. Set defaults — isBooked=false, isBlocked=false
     * 4. Save to database and return
     *
     * Why check for past date?
     * No point creating slots in the past.
     * Patients cannot book them anyway.
     */
    @Override
    public AvailabilitySlot addSlot(SlotRequest request) {

        // do not allow creating slots in the past
        // past slots are useless — patients cannot book them
        if (request.getDate().isBefore(LocalDate.now())) {
            throw new RuntimeException(
                "Cannot create slot in the past. Please select a future date."
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

        // save to database and return saved slot with generated slotId
        return slotRepository.save(slot);
    }

    /*
     * Add multiple slots at once.
     *
     * How it works:
     * Doctor sends a list of slot requests.
     * We loop through each one and save them all.
     * More efficient than calling addSlot() separately for each.
     *
     * Example use case:
     * Doctor adds slots for Monday, Wednesday, Friday
     * all at once instead of three separate API calls.
     */
    @Override
    public List<AvailabilitySlot> addBulkSlots(List<SlotRequest> requests) {

        // list to collect all saved slots
        List<AvailabilitySlot> savedSlots = new ArrayList<>();

        // loop through each request and save each slot
        for (SlotRequest request : requests) {

            // reuse addSlot() logic for each request
            // this also validates past date for each slot
            AvailabilitySlot slot = addSlot(request);
            savedSlots.add(slot);
        }

        // return all saved slots
        return savedSlots;
    }

    /*
     * Generate recurring slots automatically.
     *
     * How it works:
     * Doctor sets:
     * → start date (example: April 1)
     * → end date (example: April 30)
     * → recurrence pattern (DAILY or WEEKLY)
     * → time and duration for each slot
     *
     * DAILY  → creates one slot every day from start to end date
     * WEEKLY → creates one slot every week on same day of week
     *
     * Example:
     * Doctor sets Monday 10:00-10:30 WEEKLY from April 1 to April 30
     * → System creates: April 7, April 14, April 21, April 28
     * All automatically without doctor doing it manually.
     *
     * This saves doctor enormous time for regular schedules.
     */
    @Override
    public List<AvailabilitySlot> generateRecurringSlots(SlotRequest request) {

        // recurrence end date is required for recurring slots
        if (request.getRecurrenceEndDate() == null) {
            throw new RuntimeException(
                "Recurrence end date is required for recurring slots."
            );
        }

        // end date must be after start date
        if (request.getRecurrenceEndDate().isBefore(request.getDate())) {
            throw new RuntimeException(
                "Recurrence end date must be after start date."
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
            if (request.getRecurrence().equals("DAILY")) {
                // daily → move to next day
                currentDate = currentDate.plusDays(1);

            } else if (request.getRecurrence().equals("WEEKLY")) {
                // weekly → move to same day next week
                currentDate = currentDate.plusWeeks(1);

            } else {
                // unknown recurrence pattern → stop loop
                break;
            }
        }

        // return all generated slots
        return generatedSlots;
    }

    /*
     * Get all slots for a specific doctor.
     *
     * Doctor uses this to view their complete schedule.
     * Returns everything — booked, blocked, and available.
     * Doctor needs to see all of them to manage their calendar.
     */
    @Override
    public List<AvailabilitySlot> getSlotsByProvider(int providerId) {

        // get all slots for this doctor from database
        return slotRepository.findByProviderId(providerId);
    }

    /*
     * Get only available slots for a doctor on a specific date.
     *
     * This is what patients see when they pick a date.
     * Only slots that are:
     * → not booked (isBooked = false)
     * → not blocked (isBlocked = false)
     * are shown to patients.
     *
     * Blocked and booked slots are completely hidden from patients.
     */
    @Override
    public List<AvailabilitySlot> getAvailableSlots(
            int providerId,
            LocalDate date) {

        // get only available slots for this doctor on this date
        // repository query handles the isBooked=false and isBlocked=false filter
        return slotRepository.findAvailableByProviderAndDate(
                providerId,
                date
        );
    }

    /*
     * Get a single slot by its ID.
     *
     * Used when:
     * → Patient books an appointment (UC4 calls this)
     * → Doctor updates or deletes a specific slot
     * → Admin views slot details
     *
     * Throws exception if slot not found.
     * Exception will be handled by GlobalExceptionHandler later.
     */
    @Override
    public AvailabilitySlot getSlotById(int slotId) {

        // find slot by id — throw exception if not found
        return slotRepository.findBySlotId(slotId)
                .orElseThrow(() -> new RuntimeException(
                    "Slot not found with id: " + slotId
                ));
    }

    /*
     * Mark a slot as booked.
     *
     * This is called by AppointmentService in UC4
     * when a patient confirms their booking.
     *
     * @Transactional is very important here.
     * It means: do everything or do nothing.
     * If anything fails → database rolls back to original state.
     *
     * @Version on AvailabilitySlot handles double booking.
     * If two patients try to book same slot simultaneously:
     * → First one saves → version becomes 1
     * → Second one tries → sees version mismatch → exception
     * → Only first patient gets the slot
     * → This is the PDF data integrity requirement
     */
    @Override
    @Transactional
    public void bookSlot(int slotId) {

        // find the slot
        AvailabilitySlot slot = getSlotById(slotId);

        // check if slot is already booked
        // this is a safety check on top of optimistic locking
        if (slot.isBooked()) {
            throw new RuntimeException(
                "This slot is already booked. Please choose another slot."
            );
        }

        // check if slot is blocked by doctor
        if (slot.isBlocked()) {
            throw new RuntimeException(
                "This slot is blocked by the doctor and cannot be booked."
            );
        }

        // mark slot as booked
        slot.setBooked(true);

        // save — @Version automatically increments
        // if another transaction saved first → OptimisticLockException
        slotRepository.save(slot);
    }

    /*
     * Release a booked slot back to available.
     *
     * Called by AppointmentService in UC4
     * when patient cancels their appointment.
     *
     * Slot becomes available again immediately
     * so another patient can book it.
     */
    @Override
    @Transactional
    public void releaseSlot(int slotId) {

        // find the slot
        AvailabilitySlot slot = getSlotById(slotId);

        // set isBooked back to false
        // slot is now available for other patients to book
        slot.setBooked(false);

        // save updated slot
        slotRepository.save(slot);
    }

    /*
     * Doctor blocks a slot for personal unavailability.
     *
     * Doctor might have:
     * → A meeting during that time
     * → Personal appointment
     * → Break time
     *
     * Blocked slot becomes INVISIBLE to patients.
     * Patients cannot see or book it.
     * This is a strict PDF requirement.
     */
    @Override
    public void blockSlot(int slotId) {

        // find the slot
        AvailabilitySlot slot = getSlotById(slotId);

        // cannot block a slot that is already booked
        // patient has already booked it — cannot block now
        if (slot.isBooked()) {
            throw new RuntimeException(
                "Cannot block a slot that is already booked by a patient."
            );
        }

        // set isBlocked to true
        // slot disappears from patient view immediately
        slot.setBlocked(true);

        // save updated slot
        slotRepository.save(slot);
    }

    /*
     * Doctor unblocks a previously blocked slot.
     *
     * Doctor blocked a slot earlier but now wants
     * to make it available again.
     * Sets isBlocked back to false.
     * Slot appears in patient search immediately.
     */
    @Override
    public void unblockSlot(int slotId) {

        // find the slot
        AvailabilitySlot slot = getSlotById(slotId);

        // set isBlocked back to false
        // slot is now visible and bookable by patients
        slot.setBlocked(false);

        // save updated slot
        slotRepository.save(slot);
    }

    /*
     * Update slot details.
     *
     * Doctor wants to change the time or duration of a slot.
     * Cannot update if patient has already booked it.
     * That would be unfair to the patient who booked.
     */
    @Override
    public AvailabilitySlot updateSlot(int slotId, SlotRequest request) {

        // find existing slot
        AvailabilitySlot existing = getSlotById(slotId);

        // cannot update a slot that patient has already booked
        if (existing.isBooked()) {
            throw new RuntimeException(
                "Cannot update a slot that is already booked by a patient."
            );
        }

        // update the slot fields with new values
        existing.setDate(request.getDate());
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setDurationMinutes(request.getDurationMinutes());

        // save and return updated slot
        return slotRepository.save(existing);
    }

    /*
     * Delete a slot permanently from calendar.
     *
     * Doctor removes a slot they no longer need.
     * Cannot delete if patient has already booked it.
     * Patient would lose their appointment — not allowed.
     */
    @Override
    @Transactional
    public void deleteSlot(int slotId) {

        // find the slot first to make sure it exists
        AvailabilitySlot slot = getSlotById(slotId);

        // cannot delete a slot that patient has already booked
        if (slot.isBooked()) {
            throw new RuntimeException(
                "Cannot delete a slot that is already booked by a patient." +
                " Please cancel the appointment first."
            );
        }

        // delete slot from database
        slotRepository.deleteBySlotId(slotId);
    }

    /*
     * Delete all expired slots automatically.
     *
     * Expired slots = past date AND never booked.
     * These are useless — no patient will ever book them.
     * Keeping them wastes database space.
     *
     * This method is called by SlotExpiryScheduler
     * which runs automatically every night at midnight.
     * Doctor and patient never call this manually.
     *
     * Example:
     * Dr. Sharma had a slot on March 1 at 10:00.
     * No patient booked it. March 1 is now past.
     * This method deletes it automatically.
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