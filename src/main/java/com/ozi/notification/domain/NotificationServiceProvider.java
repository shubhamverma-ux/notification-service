package com.ozi.notification.domain;

/**
 * Service provider interface for routing notifications to appropriate services.
 * This acts as a facade for all notification services.
 */
public interface NotificationServiceProvider {

    /**
     * Sends a notification using the appropriate service based on notification type.
     *
     * @param notification The notification to send
     * @return The notification with updated status
     * @throws NotificationException if no suitable service is found or sending fails
     */
    Notification sendNotification(Notification notification) throws NotificationException;
}