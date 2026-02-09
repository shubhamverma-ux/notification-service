package com.ozi.notification.domain;

/**
 * Domain service interface for sending notifications.
 * This defines the core business capability for notification delivery.
 */
public interface NotificationService {

    /**
     * Sends a notification using the appropriate provider based on notification type.
     *
     * @param notification The notification to send
     * @return The notification with updated status
     * @throws NotificationException if sending fails
     */
    Notification sendNotification(Notification notification) throws NotificationException;

    /**
     * Checks if this service can handle the given notification type.
     *
     * @param type The notification type
     * @return true if this service can handle the type
     */
    boolean canHandle(NotificationType type);
}