package com.ozi.notification.domain;

/**
 * Service interface for sending stock notifications via CleverTap.
 */
public interface StockNotificationService {

    /**
     * Sends a back-in-stock notification to a user via CleverTap campaign trigger.
     *
     * @param event The stock notification event to send
     * @return true if the notification was sent successfully
     * @throws NotificationException if sending fails
     */
    boolean sendStockNotification(StockNotificationEvent event) throws NotificationException;
}
