package com.medibook.notification.service.impl;

import com.medibook.notification.dto.NotificationRequest;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.entity.User;
import com.medibook.exception.BadRequestException;
import com.medibook.exception.ResourceNotFoundException;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.repository.NotificationRepository;
import com.medibook.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${spring.mail.username}")
    private String senderEmail;

    /*
     * Send a single notification.
     * 1. Validate request
     * 2. Always save to database
     * 3. If channel = EMAIL → look up user email → send real email
     * 4. If channel = SMS → mock log
     */
    @Override
    public Notification send(NotificationRequest request) {

        // validate channel is one of allowed values
        String channel = request.getChannel();
        if (!channel.equals("APP")
                && !channel.equals("EMAIL")
                && !channel.equals("SMS")) {
            throw new BadRequestException(
                "Invalid channel. Allowed values: APP, EMAIL, SMS"
            );
        }

        // validate type is one of allowed values
        String type = request.getType();
        if (!type.equals("BOOKING")
                && !type.equals("REMINDER")
                && !type.equals("CANCELLATION")
                && !type.equals("PAYMENT")
                && !type.equals("FOLLOWUP")
                && !type.equals("ANNOUNCEMENT")) {
            throw new BadRequestException(
                "Invalid type. Allowed values: BOOKING, REMINDER, " +
                "CANCELLATION, PAYMENT, FOLLOWUP, ANNOUNCEMENT"
            );
        }

        // step 1 — always save to database
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .channel(request.getChannel())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // step 2 — if channel is EMAIL, look up real patient email and send
        if ("EMAIL".equals(request.getChannel()) && emailEnabled) {
            try {
                // look up actual patient email from users table
                // throws ResourceNotFoundException if user not found
                User user = userRepository
                        .findById(request.getRecipientId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                            "User", "id", request.getRecipientId()
                        ));

                sendEmail(
                    user.getEmail(),
                    request.getTitle(),
                    request.getMessage()
                );

            } catch (ResourceNotFoundException e) {
                // rethrow resource not found — caller should know
                throw e;
            } catch (Exception e) {
                // email failure should NOT fail the notification
                // notification is already saved to DB
                System.out.println("Email sending failed: "
                        + e.getMessage());
            }
        }

        // step 3 — if channel is SMS, send SMS (mock)
        if ("SMS".equals(request.getChannel()) && smsEnabled) {
            sendSms("recipient_phone", request.getMessage());
        }

        return saved;
    }

    /*
     * Send bulk notification to multiple recipients.
     * Admin platform-wide announcement.
     */
    @Override
    public void sendBulk(List<Integer> recipientIds,
                         String title, String message) {

        // validate list is not empty
        if (recipientIds == null || recipientIds.isEmpty()) {
            throw new BadRequestException(
                "Recipient list cannot be empty."
            );
        }

        // validate title and message
        if (title == null || title.trim().isEmpty()) {
            throw new BadRequestException(
                "Title cannot be empty."
            );
        }

        if (message == null || message.trim().isEmpty()) {
            throw new BadRequestException(
                "Message cannot be empty."
            );
        }

        for (int recipientId : recipientIds) {
            NotificationRequest request = new NotificationRequest();
            request.setRecipientId(recipientId);
            request.setType("ANNOUNCEMENT");
            request.setTitle(title);
            request.setMessage(message);
            request.setChannel("APP");
            send(request);
        }
    }

    /*
     * Mark a single notification as read.
     */
    @Override
    public void markAsRead(int notificationId) {

        // throws 404 Not Found if notification not found
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Notification", "id", notificationId
                ));

        // check if already read
        if (notification.isRead()) {
            throw new BadRequestException(
                "Notification is already marked as read."
            );
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /*
     * Mark ALL notifications as read for a user.
     */
    @Override
    public void markAllRead(int recipientId) {
        notificationRepository.markAllAsRead(recipientId);
    }

    /*
     * Get all notifications for a user — newest first.
     */
    @Override
    public List<Notification> getByRecipient(int recipientId) {
        return notificationRepository
                .findByRecipientIdOrderBySentAtDesc(recipientId);
    }

    /*
     * Get unread notification count for badge.
     */
    @Override
    public long getUnreadCount(int recipientId) {
        return notificationRepository
                .countByRecipientIdAndIsRead(recipientId, false);
    }

    /*
     * Delete a notification permanently.
     */
    @Override
    public void deleteNotification(int notificationId) {

        // throws 404 Not Found if notification not found
        notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Notification", "id", notificationId
                ));

        notificationRepository.deleteByNotificationId(notificationId);
    }

    /*
     * Send email via Gmail SMTP.
     * toEmail = real patient email fetched from users table.
     */
    @Override
    public void sendEmail(String toEmail,
                          String subject, String body) {

        // validate email is not empty
        if (toEmail == null || toEmail.trim().isEmpty()) {
            throw new BadRequestException(
                "Recipient email cannot be empty."
            );
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(senderEmail);
            mail.setTo(toEmail);
            mail.setSubject(subject);
            mail.setText(body);
            mailSender.send(mail);
            System.out.println("Email sent to: " + toEmail);
        } catch (Exception e) {
            System.out.println("Email failed to: " + toEmail
                    + " | Error: " + e.getMessage());
        }
    }

    /*
     * Send SMS — MOCK MODE.
     * Swap one line for Twilio when ready.
     */
    @Override
    public void sendSms(String phoneNumber, String message) {

        // validate phone number is not empty
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new BadRequestException(
                "Phone number cannot be empty."
            );
        }

        System.out.println("SMS MOCK → To: "
                + phoneNumber
                + " | Message: " + message);
        // TWILIO MODE → uncomment when ready:
        // twilioSms(phoneNumber, message);
    }

    /*
     * Get all notifications — admin view.
     */
    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAllByOrderBySentAtDesc();
    }
}