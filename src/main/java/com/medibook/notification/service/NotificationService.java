package com.medibook.notification.service;

import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.entity.Notification;

import java.util.List;

/*
 * Service interface for Notification.
 * Defines all notification operations.
 * NotificationServiceImpl provides actual logic.
 *
 * This interface is called by other services:
 * → AppointmentService calls send() after booking
 * → AppointmentService calls send() after cancellation
 * → PaymentService calls send() after payment
 * → RecordService calls send() for follow-up reminders
 * → Admin calls sendBulk() for platform announcements
 */
public interface NotificationService {

    /*
     * Send a single notification.
     * Routes to APP/EMAIL/SMS based on channel field.
     * Always saves to database regardless of channel.
     * Called by all services after key events.
     */
    Notification send(NotificationRequest request);

    /*
     * Send notification to multiple recipients at once.
     * Admin uses this for platform-wide announcements.
     * Example: "MediBook will be down for maintenance"
     * Sends same message to all recipientIds in list.
     */
    void sendBulk(List<Integer> recipientIds,
                  String title, String message);

    /*
     * Mark a single notification as read.
     * Called when patient clicks on a notification.
     * isRead → true → removed from badge count.
     */
    void markAsRead(int notificationId);

    /*
     * Mark ALL notifications as read for a user.
     * Patient clicks "Mark all as read" button.
     * Badge count → 0.
     */
    void markAllRead(int recipientId);

    /*
     * Get all notifications for a user.
     * Shown in notification centre — newest first.
     * Includes read and unread both.
     */
    List<Notification> getByRecipient(int recipientId);

    /*
     * Get count of unread notifications.
     * Shown as badge number on bell icon in UI.
     * Example: bell shows "5" unread notifications.
     */
    long getUnreadCount(int recipientId);

    /*
     * Delete a notification permanently.
     * Patient removes notification from their list.
     */
    void deleteNotification(int notificationId);

    /*
     * Send email notification via Gmail SMTP.
     * Called internally when channel = EMAIL.
     * Uses JavaMailSender configured in properties.
     */
    void sendEmail(String toEmail,
                   String subject, String body);

    /*
     * Send SMS notification.
     * MOCK MODE → logs message, does not send real SMS
     * TWILIO MODE → calls Twilio API (swap one line)
     * Called internally when channel = SMS.
     */
    void sendSms(String phoneNumber, String message);

    /*
     * Get all notifications — admin view.
     * Admin sees every notification on platform.
     */
    List<Notification> getAll();
}