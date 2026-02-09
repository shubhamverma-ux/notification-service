package com.ozi.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    void shouldCreateNotificationWithPendingStatus() {
        // Given
        NotificationType type = NotificationType.PUSH;
        String recipient = "user@example.com";
        String title = "Test Title";
        String message = "Test Message";

        // When
        Notification notification = Notification.create(type, recipient, title, message);

        // Then
        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getType()).isEqualTo(type);
        assertThat(notification.getRecipient()).isEqualTo(recipient);
        assertThat(notification.getTitle()).isEqualTo(title);
        assertThat(notification.getMessage()).isEqualTo(message);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getPriority()).isEqualTo(NotificationPriority.NORMAL);
        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getSentAt()).isNull();
        assertThat(notification.getErrorMessage()).isNull();
    }

    @Test
    void shouldMarkNotificationAsSent() {
        // Given
        Notification notification = Notification.create(
            NotificationType.PUSH,
            "user@example.com",
            "Test Title",
            "Test Message"
        );

        // When
        Notification sentNotification = notification.markAsSent();

        // Then
        assertThat(sentNotification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(sentNotification.getSentAt()).isNotNull();
        assertThat(sentNotification.getErrorMessage()).isNull();
        assertThat(sentNotification.isTerminal()).isTrue();
    }

    @Test
    void shouldMarkNotificationAsFailed() {
        // Given
        Notification notification = Notification.create(
            NotificationType.PUSH,
            "user@example.com",
            "Test Title",
            "Test Message"
        );
        String errorMessage = "Failed to send notification";

        // When
        Notification failedNotification = notification.markAsFailed(errorMessage);

        // Then
        assertThat(failedNotification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(failedNotification.getSentAt()).isNull();
        assertThat(failedNotification.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(failedNotification.isTerminal()).isTrue();
    }

    @Test
    void shouldCreateNotificationWithAllFields() {
        // Given
        String id = "test-id";
        NotificationType type = NotificationType.WHATSAPP;
        String recipient = "+1234567890";
        String title = "Welcome!";
        String message = "Welcome to our service";
        var data = java.util.Map.of("userId", "123", "action", "welcome");
        String deepLink = "https://app.example.com/welcome";
        NotificationPriority priority = NotificationPriority.HIGH;
        NotificationStatus status = NotificationStatus.SENT;
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(5);
        LocalDateTime sentAt = LocalDateTime.now();

        // When
        Notification notification = Notification.builder()
                .id(id)
                .type(type)
                .recipient(recipient)
                .title(title)
                .message(message)
                .data(data)
                .deepLink(deepLink)
                .priority(priority)
                .status(status)
                .createdAt(createdAt)
                .sentAt(sentAt)
                .build();

        // Then
        assertThat(notification.getId()).isEqualTo(id);
        assertThat(notification.getType()).isEqualTo(type);
        assertThat(notification.getRecipient()).isEqualTo(recipient);
        assertThat(notification.getTitle()).isEqualTo(title);
        assertThat(notification.getMessage()).isEqualTo(message);
        assertThat(notification.getData()).isEqualTo(data);
        assertThat(notification.getDeepLink()).isEqualTo(deepLink);
        assertThat(notification.getPriority()).isEqualTo(priority);
        assertThat(notification.getStatus()).isEqualTo(status);
        assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(notification.getSentAt()).isEqualTo(sentAt);
    }
}