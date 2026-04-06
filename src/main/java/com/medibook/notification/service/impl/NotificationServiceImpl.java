package com.medibook.notification.service.impl;

import com.medibook.notification.dto.NotificationRequest;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.entity.User;
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
     * 1. Always save to database
     * 2. If channel = EMAIL → look up user email → send real email
     * 3. If channel = SMS → mock log
     */
    @Override
    public Notification send(NotificationRequest request) {

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
                User user = userRepository
                        .findById(request.getRecipientId())
                        .orElse(null);

                if (user != null) {
                    sendEmail(
                        user.getEmail(),       // real patient email from DB
                        request.getTitle(),
                        request.getMessage()
                    );
                } else {
                    System.out.println("User not found for recipientId: "
                            + request.getRecipientId());
                }
            } catch (Exception e) {
                // email failure should NOT fail the notification
                // notification is already saved to DB
                System.out.println("Email sending failed: " + e.getMessage());
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

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new RuntimeException(
                    "Notification not found: " + notificationId
                ));

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
        notificationRepository.deleteByNotificationId(notificationId);
    }

    /*
     * Send email via Gmail SMTP.
     * toEmail = real patient email fetched from users table.
     */
    @Override
    public void sendEmail(String toEmail, String subject, String body) {
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