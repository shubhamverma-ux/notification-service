package com.ozi.notification.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain entity representing a notification request.
 * This is the core business object in the notification domain.
 */
@Value
@Builder
public class Notification {

    /**
     * Unique identifier for the notification
     */
    String id;

    /**
     * Type of notification (PUSH, WHATSAPP, EMAIL, etc.)
     */
    NotificationType type;

    /**
     * Recipient identifier (email, phone number, user ID, etc.)
     */
    String recipient;

    /**
     * Notification title/subject
     */
    String title;

    /**
     * Notification message content
     */
    String message;

    /**
     * Additional data payload
     */
    Map<String, String> data;

    /**
     * Deep link URL for the notification
     */
    String deepLink;

    /**
     * Priority level of the notification
     */
    NotificationPriority priority;

    /**
     * Current status of the notification
     */
    NotificationStatus status;

    /**
     * Timestamp when the notification was created
     */
    LocalDateTime createdAt;

    /**
     * Timestamp when the notification was sent
     */
    LocalDateTime sentAt;

    /**
     * Error message if sending failed
     */
    String errorMessage;

    /**
     * Additional metadata for the notification
     */
    Map<String, Object> metadata;

    /**
     * Creates a new notification with PENDING status
     */
    public static Notification create(NotificationType type, String recipient, String title, String message) {
        return Notification.builder()
                .id(java.util.UUID.randomUUID().toString())
                .type(type)
                .recipient(recipient)
                .title(title)
                .message(message)
                .priority(NotificationPriority.NORMAL)
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Marks the notification as sent
     */
    public Notification markAsSent() {
        return Notification.builder()
                .id(this.id)
                .type(this.type)
                .recipient(this.recipient)
                .title(this.title)
                .message(this.message)
                .data(this.data)
                .deepLink(this.deepLink)
                .priority(this.priority)
                .status(NotificationStatus.SENT)
                .createdAt(this.createdAt)
                .sentAt(LocalDateTime.now())
                .errorMessage(null)
                .metadata(this.metadata)
                .build();
    }

    /**
     * Marks the notification as failed with an error message
     */
    public Notification markAsFailed(String errorMessage) {
        return Notification.builder()
                .id(this.id)
                .type(this.type)
                .recipient(this.recipient)
                .title(this.title)
                .message(this.message)
                .data(this.data)
                .deepLink(this.deepLink)
                .priority(this.priority)
                .status(NotificationStatus.FAILED)
                .createdAt(this.createdAt)
                .sentAt(this.sentAt)
                .errorMessage(errorMessage)
                .metadata(this.metadata)
                .build();
    }

    /**
     * Checks if the notification is in a terminal state
     */
    public boolean isTerminal() {
        return status == NotificationStatus.SENT || status == NotificationStatus.FAILED;
    }
}