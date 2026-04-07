package com.medibook.scheduler;

import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;
import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.service.NotificationService;
import com.medibook.record.entity.MedicalRecord;
import com.medibook.record.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/*
 * Runs every morning at 8:00 AM automatically.
 * Checks all medical records for today's follow-up date.
 * Sends real email notification to patient automatically.
 *
 * PDF requirement:
 * "Records with a follow-up date trigger an automated
 * reminder notification to the patient on that date."
 *
 * Why needed?
 * Doctor sets follow-up date = April 25 in medical record.
 * On April 25 at 8:00 AM → scheduler wakes up automatically.
 * Finds all records with followUpDate = today.
 * Sends real email to each patient.
 * Patient gets reminder without anyone doing anything manually.
 *
 * This connects UC8 (Medical Records) → UC7 (Notifications).
 * Real email sent via JavaMailSender configured in UC7.
 */
@Component
public class FollowUpReminderScheduler {

    /*
     * RecordService — get all records with today's follow-up date.
     * Already implemented in UC8.
     */
    @Autowired
    private RecordService recordService;

    /*
     * NotificationService — send real email to patient.
     * Already implemented in UC7 with Gmail SMTP.
     */
    @Autowired
    private NotificationService notificationService;

    /*
     * UserRepository — look up patient email by patientId.
     * Already implemented in UC1.
     */
    @Autowired
    private UserRepository userRepository;

    /*
     * Cron expression: "0 0 8 * * *"
     * Runs at 08:00:00 every day.
     *
     * Cron format: second minute hour day month weekday
     * 0  = at second 0
     * 0  = at minute 0
     * 8  = at hour 8 (8 AM)
     * *  = every day
     * *  = every month
     * *  = any weekday
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendFollowUpReminders() {

        System.out.println("FollowUpReminderScheduler → " +
                "Running at 8 AM. Checking follow-up records...");

        try {
            LocalDate today = LocalDate.now();

            // get all medical records with followUpDate = today
            // already implemented in RecordServiceImpl UC8
            List<MedicalRecord> recordsDueToday =
                    recordService.getFollowUpRecords(today);

            System.out.println("FollowUpReminderScheduler → " +
                    "Records with follow-up today: "
                    + recordsDueToday.size());

            // send reminder to each patient
            for (MedicalRecord record : recordsDueToday) {

                try {
                    // look up patient from users table
                    User patient = userRepository
                            .findById(record.getPatientId())
                            .orElse(null);

                    if (patient == null) {
                        System.out.println(
                            "FollowUpReminderScheduler → " +
                            "Patient not found for recordId: "
                            + record.getRecordId()
                        );
                        continue;
                    }

                    // build notification request
                    // channel = EMAIL → real email via Gmail SMTP
                    NotificationRequest request =
                            new NotificationRequest();
                    request.setRecipientId(record.getPatientId());
                    request.setType("FOLLOWUP");
                    request.setTitle("Follow-Up Reminder 🏥");
                    request.setMessage(
                        "Dear " + patient.getFullName() + ", " +
                        "this is a reminder that today is your " +
                        "scheduled follow-up date. " +
                        "Please contact your doctor or visit " +
                        "the clinic. " +
                        "Diagnosis: " + record.getDiagnosis() +
                        ". - MediBook Team"
                    );
                    request.setChannel("EMAIL");
                    request.setRelatedId(record.getRecordId());
                    request.setRelatedType("RECORD");

                    // send notification
                    // UC7 handles real email delivery automatically
                    notificationService.send(request);

                    System.out.println(
                        "FollowUpReminderScheduler → " +
                        "Reminder sent to: " + patient.getEmail() +
                        " for recordId: " + record.getRecordId()
                    );

                } catch (Exception e) {
                    // one patient failure should NOT stop others
                    System.err.println(
                        "FollowUpReminderScheduler → " +
                        "Failed for recordId: "
                        + record.getRecordId()
                        + " | Error: " + e.getMessage()
                    );
                }
            }

            System.out.println("FollowUpReminderScheduler → " +
                    "Done. All follow-up reminders processed.");

        } catch (Exception e) {
            System.err.println("FollowUpReminderScheduler → " +
                    "Fatal error: " + e.getMessage());
        }
    }
}