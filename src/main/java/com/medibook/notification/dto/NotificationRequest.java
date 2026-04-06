package com.medibook.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/*
 * DTO for sending a notification.
 * Used when any service wants to send a notification.
 * Example: AppointmentService sends booking confirmation
 * Example: Admin sends platform-wide message
 */
@Data
public class NotificationRequest {

    /*
     * Who receives this notification?
     * Links to userId in users table (UC1).
     */
    @NotNull(message = "Recipient ID is required")
    private int recipientId;

    /*
     * What type of notification is this?
     * PDF defines exactly these types:
     * BOOKING     → appointment booked
     * REMINDER    → 24hr or 1hr before appointment
     * CANCELLATION → appointment cancelled
     * PAYMENT     → payment receipt
     * FOLLOWUP    → follow-up date reminder from UC8
     */
    @NotBlank(message = "Notification type is required")
    private String type;

    /*
     * Short heading of notification.
     * Example: "Appointment Confirmed"
     * Example: "Reminder: Your appointment is in 1 hour"
     * Shown as notification title in bell icon.
     */
    @NotBlank(message = "Title is required")
    private String title;

    /*
     * Full notification message.
     * Example: "Your appointment with Dr. Sharma is
     * confirmed for April 10 at 10:00 AM"
     */
    @NotBlank(message = "Message is required")
    private String message;

    /*
     * Which channel to send on?
     * APP   → in-app notification (stored in DB)
     * EMAIL → send via Gmail SMTP
     * SMS   → mock now, Twilio later
     * Default is APP.
     */
    private String channel = "APP";

    /*
     * Which record does this relate to?
     * Example: appointmentId = 5
     * Used for deep linking in notification centre.
     * Patient clicks notification → goes to appointment 5
     */
    private int relatedId;

    /*
     * What type of record does relatedId point to?
     * Example: "APPOINTMENT", "PAYMENT", "RECORD"
     * Used with relatedId for deep linking.
     */
    private String relatedType;
}