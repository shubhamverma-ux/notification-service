package com.ozi.notification.application.dto;

import com.ozi.notification.domain.NotificationStatus;
import lombok.Builder;
import lombok.Value;

/**
 * DTO for notification sending responses.
 */
@Value
@Builder
public class SendNotificationResponseDto {

    String notificationId;

    NotificationStatus status;

    String errorMessage;

    boolean success;

    String timestamp;
}