package com.medibook.schedule.resource;

import com.medibook.schedule.dto.SlotRequest;
import com.medibook.schedule.entity.AvailabilitySlot;
import com.medibook.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/*
 * This is the REST Controller for Schedule.
 *
 * Think of it like the front desk of a doctor's clinic.
 * Patient or doctor comes to the desk (this controller),
 * desk person (controller) calls the relevant staff (service),
 * staff does the work and sends response back.
 *
 * This class only does two things:
 * 1. Receive HTTP requests
 * 2. Call the right service method and return response
 *
 * Zero business logic here — that lives in ScheduleServiceImpl.
 * Thin controller, thick service — good design principle.
 *
 * @RestController → handles REST API requests, returns JSON
 * @RequestMapping → all URLs in this class start with /slots
 */
@RestController
@RequestMapping("/slots")
public class ScheduleResource {

    /*
     * Inject ScheduleService — Spring does this automatically.
     * We depend on the interface, not the implementation.
     * This is loose coupling — a good design principle.
     */
    @Autowired
    private ScheduleService scheduleService;

    /*
     * Add a single time slot.
     *
     * Who calls this: Doctor adding one specific slot to calendar
     * What happens: Creates one slot in availability_slots table
     * Why POST: We are creating a new resource
     *
     * URL: POST /slots/add
     * Body: SlotRequest (providerId, date, startTime, endTime, duration)
     * Returns: 201 Created with the saved slot details
     */
    @PostMapping("/add")
    public ResponseEntity<AvailabilitySlot> addSlot(
            @Valid @RequestBody SlotRequest request) {

        // call service to add one slot
        AvailabilitySlot slot = scheduleService.addSlot(request);

        // return 201 Created with saved slot
        return ResponseEntity.status(HttpStatus.CREATED).body(slot);
    }

    /*
     * Add multiple slots at once.
     *
     * Who calls this: Doctor adding many slots in one go
     * What happens: Creates all slots in one API call
     * Why POST: We are creating new resources
     *
     * URL: POST /slots/bulk
     * Body: List of SlotRequest objects
     * Returns: 201 Created with list of all saved slots
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<AvailabilitySlot>> addBulkSlots(
            @RequestBody List<@Valid SlotRequest> requests) {

        // call service to add multiple slots at once
        List<AvailabilitySlot> slots = scheduleService.addBulkSlots(requests);

        // return 201 Created with all saved slots
        return ResponseEntity.status(HttpStatus.CREATED).body(slots);
    }

    /*
     * Generate recurring slots automatically.
     *
     * Who calls this: Doctor setting up a weekly or daily schedule
     * What happens: System creates all slots from start to end date
     * Why POST: We are creating new resources
     *
     * URL: POST /slots/recurring
     * Body: SlotRequest with recurrence=DAILY or WEEKLY
     *       and recurrenceEndDate set
     * Returns: 201 Created with all generated slots
     *
     * Example:
     * Doctor wants 10:00-10:30 slots every Monday in April
     * → sends one request → system creates April 7, 14, 21, 28
     */
    @PostMapping("/recurring")
    public ResponseEntity<List<AvailabilitySlot>> generateRecurring(
            @Valid @RequestBody SlotRequest request) {

        // call service to generate recurring slots
        List<AvailabilitySlot> slots =
                scheduleService.generateRecurringSlots(request);

        // return 201 Created with all generated slots
        return ResponseEntity.status(HttpStatus.CREATED).body(slots);
    }

    /*
     * Get all slots for a specific doctor.
     *
     * Who calls this: Doctor viewing their complete schedule
     *                 Admin viewing a doctor's full calendar
     * What happens: Returns all slots — booked, blocked, available
     * Why GET: We are reading resources
     *
     * URL: GET /slots/provider/{providerId}
     * Returns: 200 OK with all slots for that doctor
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<AvailabilitySlot>> getByProvider(
            @PathVariable int providerId) {

        // call service to get all slots for this doctor
        return ResponseEntity.ok(
                scheduleService.getSlotsByProvider(providerId)
        );
    }

    /*
     * Get available slots for a doctor on a specific date.
     *
     * Who calls this: Patient selecting a date to book appointment
     * What happens: Returns only unbooked and unblocked slots
     * Why GET: We are reading resources
     *
     * URL: GET /slots/available/{providerId}?date=2026-04-10
     * Returns: 200 OK with list of available slots
     *
     * This is the most important endpoint for patients.
     * They pick a doctor → pick a date → see these slots
     * → pick one slot → book appointment (UC4)
     */
    @GetMapping("/available/{providerId}")
    public ResponseEntity<List<AvailabilitySlot>> getAvailable(
            @PathVariable int providerId,
            @RequestParam LocalDate date) {

        // call service to get available slots for this doctor on this date
        return ResponseEntity.ok(
                scheduleService.getAvailableSlots(providerId, date)
        );
    }

    /*
     * Get a single slot by its ID.
     *
     * Who calls this: Admin or doctor viewing one specific slot
     * What happens: Returns full slot details
     * Why GET: We are reading a resource
     *
     * URL: GET /slots/{slotId}
     * Returns: 200 OK with slot details
     */
    @GetMapping("/{slotId}")
    public ResponseEntity<AvailabilitySlot> getById(
            @PathVariable int slotId) {

        // call service to get slot by id
        return ResponseEntity.ok(
                scheduleService.getSlotById(slotId)
        );
    }

    /*
     * Update a slot's details.
     *
     * Who calls this: Doctor changing time or duration of a slot
     * What happens: Updates the slot if not already booked
     * Why PUT: We are updating an existing resource
     *
     * URL: PUT /slots/{slotId}
     * Body: SlotRequest with updated details
     * Returns: 200 OK with updated slot
     */
    @PutMapping("/{slotId}")
    public ResponseEntity<AvailabilitySlot> updateSlot(
            @PathVariable int slotId,
            @Valid @RequestBody SlotRequest request) {

        // call service to update the slot
        return ResponseEntity.ok(
                scheduleService.updateSlot(slotId, request)
        );
    }

    /*
     * Block a slot.
     *
     * Who calls this: Doctor blocking a slot for personal reasons
     * What happens: Sets isBlocked=true, slot disappears from patient view
     * Why PUT: We are updating an existing resource
     *
     * URL: PUT /slots/{slotId}/block
     * Returns: 200 OK with success message
     */
    @PutMapping("/{slotId}/block")
    public ResponseEntity<?> blockSlot(
            @PathVariable int slotId) {

        // call service to block the slot
        scheduleService.blockSlot(slotId);

        // return success message
        return ResponseEntity.ok(Map.of(
                "message", "Slot blocked successfully. " +
                           "It is now invisible to patients."
        ));
    }

    /*
     * Unblock a slot.
     *
     * Who calls this: Doctor making a blocked slot available again
     * What happens: Sets isBlocked=false, slot reappears for patients
     * Why PUT: We are updating an existing resource
     *
     * URL: PUT /slots/{slotId}/unblock
     * Returns: 200 OK with success message
     */
    @PutMapping("/{slotId}/unblock")
    public ResponseEntity<?> unblockSlot(
            @PathVariable int slotId) {

        // call service to unblock the slot
        scheduleService.unblockSlot(slotId);

        // return success message
        return ResponseEntity.ok(Map.of(
                "message", "Slot unblocked successfully. " +
                           "It is now visible and bookable by patients."
        ));
    }

    /*
     * Delete a slot permanently.
     *
     * Who calls this: Doctor removing a slot from their calendar
     * What happens: Deletes slot if not booked by a patient
     * Why DELETE: We are deleting a resource
     *
     * URL: DELETE /slots/{slotId}
     * Returns: 200 OK with success message
     */
    @DeleteMapping("/{slotId}")
    public ResponseEntity<?> deleteSlot(
            @PathVariable int slotId) {

        // call service to delete the slot
        scheduleService.deleteSlot(slotId);

        // return success message
        return ResponseEntity.ok(Map.of(
                "message", "Slot deleted successfully."
        ));
    }
}