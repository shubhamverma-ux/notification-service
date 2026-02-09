package com.ozi.notification.application.dto;

import com.ozi.notification.domain.NotificationPriority;
import com.ozi.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Value;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * DTO for sending notification requests.
 */
@Value
@Builder
public class SendNotificationRequestDto {

    @NotNull(message = "Notification type is required")
    NotificationType type;

    @NotBlank(message = "Recipient is required")
    String recipient;

    @NotBlank(message = "Title is required")
    String title;

    @NotBlank(message = "Message is required")
    String message;

    Map<String, String> data;

    String deepLink;

    @Builder.Default
    NotificationPriority priority = NotificationPriority.NORMAL;
}