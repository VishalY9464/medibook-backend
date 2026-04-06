package com.medibook.notification.resource;

import com.medibook.notification.dto.NotificationRequest;
import com.medibook.notification.entity.Notification;
import com.medibook.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
 * REST Controller for Notification.
 * Thin controller — all logic in NotificationServiceImpl.
 * Base URL: /notifications
 */
@RestController
@RequestMapping("/notifications")
public class NotificationResource {

    @Autowired
    private NotificationService notificationService;

    /*
     * Send a single notification.
     * Who calls: Any service or admin
     * When: After booking, payment, cancellation etc.
     *
     * POST /notifications/send
     */
    @PostMapping("/send")
    public ResponseEntity<Notification> send(
            @Valid @RequestBody NotificationRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(notificationService.send(request));
    }

    /*
     * Send bulk notification to multiple users.
     * Who calls: Admin
     * When: Platform-wide announcement
     *
     * POST /notifications/bulk
     * Body: { recipientIds: [1,2,3], title: "...", message: "..." }
     */
    @PostMapping("/bulk")
    public ResponseEntity<?> sendBulk(
            @RequestBody Map<String, Object> body) {

        // extract fields from request body
        @SuppressWarnings("unchecked")
        List<Integer> recipientIds =
                (List<Integer>) body.get("recipientIds");
        String title = (String) body.get("title");
        String message = (String) body.get("message");

        // send bulk notification
        notificationService.sendBulk(
                recipientIds, title, message);

        return ResponseEntity.ok(Map.of(
                "message", "Bulk notification sent to "
                        + recipientIds.size() + " users."
        ));
    }

    /*
     * Get all notifications for a user.
     * Who calls: Patient/Provider viewing notification centre
     * When: Patient clicks bell icon
     *
     * GET /notifications/recipient/{recipientId}
     */
    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Notification>> getByRecipient(
            @PathVariable int recipientId) {

        return ResponseEntity.ok(
                notificationService.getByRecipient(recipientId)
        );
    }

    /*
     * Get unread notification count for badge.
     * Who calls: UI on every page load
     * When: Shows red number on bell icon
     *
     * GET /notifications/unread/count/{recipientId}
     */
    @GetMapping("/unread/count/{recipientId}")
    public ResponseEntity<?> getUnreadCount(
            @PathVariable int recipientId) {

        return ResponseEntity.ok(Map.of(
                "recipientId", recipientId,
                "unreadCount",
                notificationService.getUnreadCount(recipientId)
        ));
    }

    /*
     * Mark single notification as read.
     * Who calls: Patient
     * When: Patient clicks on a notification
     *
     * PUT /notifications/{notificationId}/read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable int notificationId) {

        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of(
                "message", "Notification marked as read."
        ));
    }

    /*
     * Mark ALL notifications as read.
     * Who calls: Patient
     * When: Patient clicks "Mark all as read"
     *
     * PUT /notifications/read/all/{recipientId}
     */
    @PutMapping("/read/all/{recipientId}")
    public ResponseEntity<?> markAllRead(
            @PathVariable int recipientId) {

        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok(Map.of(
                "message", "All notifications marked as read."
        ));
    }

    /*
     * Delete a notification.
     * Who calls: Patient
     * When: Patient removes notification from list
     *
     * DELETE /notifications/{notificationId}
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> delete(
            @PathVariable int notificationId) {

        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of(
                "message", "Notification deleted."
        ));
    }

    /*
     * Get all notifications — admin view.
     * Who calls: Admin
     * When: Admin views platform notification log
     *
     * GET /notifications/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll() {

        return ResponseEntity.ok(
                notificationService.getAll()
        );
    }
}