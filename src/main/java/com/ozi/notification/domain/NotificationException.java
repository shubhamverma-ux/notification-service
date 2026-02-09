package com.ozi.notification.domain;

/**
 * Domain exception for notification-related errors.
 */
public class NotificationException extends Exception {

    private final String notificationId;
    private final NotificationType type;

    public NotificationException(String message) {
        super(message);
        this.notificationId = null;
        this.type = null;
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
        this.notificationId = null;
        this.type = null;
    }

    public NotificationException(String message, String notificationId, NotificationType type) {
        super(message);
        this.notificationId = notificationId;
        this.type = type;
    }

    public NotificationException(String message, String notificationId, NotificationType type, Throwable cause) {
        super(message, cause);
        this.notificationId = notificationId;
        this.type = type;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public NotificationType getType() {
        return type;
    }
}