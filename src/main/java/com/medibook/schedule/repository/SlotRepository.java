package com.medibook.schedule.repository;

import com.medibook.schedule.entity.AvailabilitySlot;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/*
 * This is the Repository for AvailabilitySlot entity.
 *
 * Think of it like the receptionist who manages the doctor's diary.
 * We ask the receptionist questions like:
 * "What slots does Dr. Sharma have on April 5?"
 * "Which slots are still available for booking?"
 * "How many free slots does Dr. Sharma have this week?"
 *
 * Spring Data JPA automatically writes the SQL for us.
 * We just write method names in a specific pattern
 * and Spring figures out the query behind the scenes.
 */
@Repository
public interface SlotRepository extends JpaRepository<AvailabilitySlot, Integer> {

    /*
     * Get all slots for a specific doctor.
     * Used by doctor to view their complete schedule.
     * Includes booked, blocked, and available slots.
     * Admin also uses this to see doctor's full calendar.
     */
    List<AvailabilitySlot> findByProviderId(int providerId);

    /*
     * Get all slots for a specific doctor on a specific date.
     * Used when patient selects a doctor and picks a date.
     * Returns everything including booked and blocked.
     * We filter available ones in service layer.
     */
    List<AvailabilitySlot> findByProviderIdAndDate(
            int providerId,
            LocalDate date
    );

    /*
     * Get only available slots for a doctor on a specific date.
     * This is the main query patients use.
     * isBooked=false means no patient booked it yet.
     * isBlocked=false means doctor has not blocked it.   
     * Both must be false for slot to be available. 
     *
     * We use @Query because the method name would be
     * too long using Spring naming convention.
     */
    @Query("SELECT s FROM AvailabilitySlot s WHERE " +
           "s.providerId = :providerId " +
           "AND s.date = :date " +
           "AND s.isBooked = false " +
           "AND s.isBlocked = false")
    List<AvailabilitySlot> findAvailableByProviderAndDate(
            @Param("providerId") int providerId,
            @Param("date") LocalDate date
    );

    /*
     * Get all slots between two dates for a doctor.
     * Used when doctor wants to see their weekly schedule.
     * Also used by scheduler to find expired slots to delete.
     * Example: find all slots from April 1 to April 7.
     */
    List<AvailabilitySlot> findByProviderIdAndDateBetween(
            int providerId,
            LocalDate startDate,
            LocalDate endDate
    );

    /*
     * Find a specific slot by its ID.
     * Used when booking, blocking, or updating a slot.
     * Returns Optional because slot might not exist.
     */
    Optional<AvailabilitySlot> findBySlotId(int slotId);

    /*
     * Count how many available slots a doctor has.
     * Used in doctor profile to show
     * "15 slots available this week"
     * Only counts unbooked and unblocked slots.
     */
    @Query("SELECT COUNT(s) FROM AvailabilitySlot s WHERE " +
           "s.providerId = :providerId " +
           "AND s.isBooked = false " +
           "AND s.isBlocked = false")
    long countAvailableByProviderId(
            @Param("providerId") int providerId
    );

    /*
     * Delete a specific slot by its ID.
     * Used when doctor removes a slot from their calendar.
     * Also used by scheduler to delete expired slots.
     */
    @Modifying
    @Transactional
    void deleteBySlotId(int slotId);

    /*
     * Find all slots on a specific date across all doctors.
     * Used by admin to see all appointments on a given day.
     * Also used by scheduler to find and delete expired slots.
     */
    List<AvailabilitySlot> findByDate(LocalDate date);

    /*
     * Find all expired slots that were never booked.
     * Expired means date is before today AND isBooked=false.
     * Used by SlotExpiryScheduler to clean up old slots.
     * Keeps database clean and performant.
     */
    @Query("SELECT s FROM AvailabilitySlot s WHERE " +
           "s.date < :today " +
           "AND s.isBooked = false")
    List<AvailabilitySlot> findExpiredSlots(
            @Param("today") LocalDate today
    );  
}