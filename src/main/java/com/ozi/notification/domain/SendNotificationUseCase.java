package com.ozi.notification.domain;

/**
 * Use case for sending notifications.
 * This represents the main business operation of sending a notification.
 */
public interface SendNotificationUseCase {

    /**
     * Executes the send notification use case.
     *
     * @param request The request containing notification details
     * @return The result of the notification sending operation
     */
    SendNotificationResult execute(SendNotificationRequest request);

    /**
     * Request object for sending notifications.
     */
    interface SendNotificationRequest {
        NotificationType getType();
        String getRecipient();
        String getTitle();
        String getMessage();
        java.util.Map<String, String> getData();
        String getDeepLink();
        NotificationPriority getPriority();
    }

    /**
     * Result object for notification sending operations.
     */
    interface SendNotificationResult {
        String getNotificationId();
        NotificationStatus getStatus();
        String getErrorMessage();
        boolean isSuccess();
    }
}