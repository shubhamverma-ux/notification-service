package com.ozi.notification.application.dto;

import com.ozi.notification.domain.Notification;
import com.ozi.notification.domain.NotificationPriority;
import com.ozi.notification.domain.NotificationStatus;
import com.ozi.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO representing a notification for API responses.
 */
@Value
@Builder
public class NotificationDto {

    String id;

    NotificationType type;

    String recipient;

    String title;

    String message;

    Map<String, String> data;

    String deepLink;

    NotificationPriority priority;

    NotificationStatus status;

    LocalDateTime createdAt;

    LocalDateTime sentAt;

    String errorMessage;

    Map<String, Object> metadata;

    /**
     * Converts a domain Notification to a NotificationDto.
     */
    public static NotificationDto fromDomain(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .recipient(notification.getRecipient())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getData())
                .deepLink(notification.getDeepLink())
                .priority(notification.getPriority())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .errorMessage(notification.getErrorMessage())
                .metadata(notification.getMetadata())
                .build();
    }
}