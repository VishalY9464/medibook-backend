package com.medibook.scheduler;

import com.medibook.schedule.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * Runs every night at midnight automatically.
 * Deletes all expired slots from database.
 *
 * Expired slot = past date AND never booked.
 * PDF requirement:
 * "Expired slots are automatically purged by a scheduled job."
 *
 * Why needed?
 * Doctor creates slots for April 10.
 * April 10 passes, nobody booked it.
 * Slot is now useless — taking space in DB.
 * This scheduler cleans it up automatically every night.
 */
@Component
public class SlotExpiryScheduler {

    /*
     * ScheduleService handles the actual deletion logic.
     * deleteExpiredSlots() is already implemented in UC3.
     * Scheduler just calls it on a schedule.
     */
    @Autowired
    private ScheduleService scheduleService;

    /*
     * Cron expression: "0 0 0 * * *"
     * Runs at 00:00:00 every day (midnight).
     *
     * Cron format: second minute hour day month weekday
     * 0        = at second 0
     * 0        = at minute 0
     * 0        = at hour 0 (midnight)
     * *        = every day
     * *        = every month
     * *        = any day of week
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void purgeExpiredSlots() {

        System.out.println("SlotExpiryScheduler → " +
                "Running at midnight. Deleting expired slots...");

        try {
            // call ScheduleService to delete all expired slots
            // expired = past date AND isBooked = false
            scheduleService.deleteExpiredSlots();

            System.out.println("SlotExpiryScheduler → " +
                    "Expired slots deleted successfully.");

        } catch (Exception e) {
            // log error but do NOT crash the scheduler
            // next midnight it will run again automatically
            System.err.println("SlotExpiryScheduler → " +
                    "Error deleting expired slots: "
                    + e.getMessage());
        }
    }
}