package com.ozi.notification.domain;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for notification persistence operations.
 * This defines the contract for storing and retrieving notifications.
 */
public interface NotificationRepository {

    /**
     * Saves a notification to the repository.
     *
     * @param notification The notification to save
     * @return The saved notification
     */
    Notification save(Notification notification);

    /**
     * Finds a notification by its ID.
     *
     * @param id The notification ID
     * @return Optional containing the notification if found
     */
    Optional<Notification> findById(String id);

    /**
     * Finds all notifications with the given status.
     *
     * @param status The notification status
     * @return List of notifications with the given status
     */
    List<Notification> findByStatus(NotificationStatus status);

    /**
     * Finds notifications by recipient.
     *
     * @param recipient The recipient identifier
     * @return List of notifications for the recipient
     */
    List<Notification> findByRecipient(String recipient);

    /**
     * Finds pending notifications that are ready to be processed.
     *
     * @return List of pending notifications
     */
    List<Notification> findPendingNotifications();

    /**
     * Updates the status of a notification.
     *
     * @param id The notification ID
     * @param status The new status
     * @return true if the update was successful
     */
    boolean updateStatus(String id, NotificationStatus status);

    /**
     * Updates the status of a notification with an error message.
     *
     * @param id The notification ID
     * @param status The new status
     * @param errorMessage The error message
     * @return true if the update was successful
     */
    boolean updateStatusWithError(String id, NotificationStatus status, String errorMessage);
}