package com.medibook.notification.repository;

import com.medibook.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * Repository for Notification entity.
 * Handles all database queries for notifications table.
 */
@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Integer> {

    /*
     * Get all notifications for a user.
     * Shown in notification centre — newest first.
     * Patient sees all their notifications here.
     */
    List<Notification> findByRecipientIdOrderBySentAtDesc(
            int recipientId);

    /*
     * Get only unread notifications for a user.
     * Used to show unread notifications list.
     */
    List<Notification> findByRecipientIdAndIsRead(
            int recipientId, boolean isRead);

    /*
     * Count unread notifications for badge.
     * Shown as red number on bell icon in UI.
     * Example: bell icon shows "3" unread.
     */
    long countByRecipientIdAndIsRead(
            int recipientId, boolean isRead);

    /*
     * Get all notifications by type.
     * Example: get all REMINDER notifications
     * Used by admin analytics.
     */
    List<Notification> findByType(String type);

    /*
     * Get notifications by relatedId.
     * Example: get all notifications for appointment 5
     * Used when viewing appointment details.
     */
    List<Notification> findByRelatedId(int relatedId);

    /*
     * Mark all notifications as read for a user.
     * Patient clicks "Mark all as read" button.
     * Badge count goes to zero.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true " +
           "WHERE n.recipientId = :recipientId")
    void markAllAsRead(@Param("recipientId") int recipientId);

    /*
     * Delete notification by its ID.
     * Patient deletes a notification from their list.
     */
    @Modifying
    @Transactional
    void deleteByNotificationId(int notificationId);

    /*
     * Get all notifications — admin view.
     * Admin sees all platform notifications.
     * Ordered newest first.
     */
    List<Notification> findAllByOrderBySentAtDesc();
}