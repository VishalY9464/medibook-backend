package com.medibook.scheduler;

import com.medibook.appointment.entity.Appointment;
import com.medibook.appointment.repository.AppointmentRepository;
import com.medibook.appointment.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/*
 * Runs every hour automatically.
 * Detects appointments that were never completed
 * and marks them as NO_SHOW.
 *
 * PDF requirement:
 * "Flag appointments as No-Show if not completed
 * within a time window."
 *
 * Why needed?
 * Patient books appointment for April 10 at 10:00 AM.
 * April 10 passes. Nobody marked it COMPLETED.
 * System automatically marks it NO_SHOW.
 * Doctor sees NO_SHOW in their records.
 * Patient cannot review a NO_SHOW appointment.
 *
 * Logic:
 * Find all SCHEDULED appointments where:
 * → appointmentDate is BEFORE today
 * OR → appointmentDate is TODAY and endTime is BEFORE now
 * → status is still SCHEDULED (not completed, not cancelled)
 * → mark all of them as NO_SHOW
 */
@Component
public class NoShowDetectionScheduler {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentService appointmentService;

    /*
     * Cron expression: "0 0 * * * *"
     * Runs at minute 0 of every hour.
     * Example: 1:00, 2:00, 3:00 ... 23:00, 00:00
     *
     * Cron format: second minute hour day month weekday
     * 0  = at second 0
     * 0  = at minute 0
     * *  = every hour
     * *  = every day
     * *  = every month
     * *  = any weekday
     */
    @Scheduled(cron = "0 0 * * * *")
    public void detectNoShows() {

        System.out.println("NoShowDetectionScheduler → " +
                "Running. Checking for no-show appointments...");

        try {
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            // get all appointments still in SCHEDULED status
            List<Appointment> scheduledAppointments =
                    appointmentRepository.findByStatus("SCHEDULED");

            int noShowCount = 0;

            for (Appointment appointment : scheduledAppointments) {

                boolean isNoShow = false;

                // case 1 — appointment date is in the past
                // appointment was yesterday or earlier → NO_SHOW
                if (appointment.getAppointmentDate()
                        .isBefore(today)) {
                    isNoShow = true;
                }

                // case 2 — appointment is today but end time passed
                // appointment was today at 10:30 AM, now it is 11:00 AM
                // doctor never marked it complete → NO_SHOW
                if (appointment.getAppointmentDate()
                        .isEqual(today)
                        && appointment.getEndTime()
                            .isBefore(now)) {
                    isNoShow = true;
                }

                // mark as NO_SHOW if either condition is true
                if (isNoShow) {
                    appointmentService.updateStatus(
                            appointment.getAppointmentId(),
                            "NO_SHOW"
                    );
                    noShowCount++;

                    System.out.println(
                        "NoShowDetectionScheduler → " +
                        "Marked NO_SHOW: appointmentId = "
                        + appointment.getAppointmentId()
                    );
                }
            }

            System.out.println("NoShowDetectionScheduler → " +
                    "Done. Total NO_SHOW marked: " + noShowCount);

        } catch (Exception e) {
            // log error but do NOT crash the scheduler
            System.err.println("NoShowDetectionScheduler → " +
                    "Error: " + e.getMessage());
        }
    }
}