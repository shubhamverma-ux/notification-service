package com.ozi.notification.application.usecase;

import com.ozi.notification.application.dto.SendNotificationRequestDto;
import com.ozi.notification.application.dto.SendNotificationResponseDto;
import com.ozi.notification.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of the SendNotificationUseCase.
 * This orchestrates the notification sending process using domain services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SendNotificationUseCaseImpl implements SendNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final NotificationServiceProvider notificationServiceProvider;

    @Override
    public SendNotificationResult execute(SendNotificationRequest request) {
        log.info("Executing send notification use case for type: {} to recipient: {}",
                request.getType(), request.getRecipient());

        try {
            // Create notification domain object
            Notification notification = Notification.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .type(request.getType())
                    .recipient(request.getRecipient())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .data(request.getData())
                    .deepLink(request.getDeepLink())
                    .priority(request.getPriority())
                    .status(NotificationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Save to repository first
            Notification savedNotification = notificationRepository.save(notification);
            log.debug("Saved notification with ID: {}", savedNotification.getId());

            // Send the notification
            Notification sentNotification = notificationServiceProvider.sendNotification(savedNotification);

            // Update the repository with the result
            Notification updatedNotification = notificationRepository.save(sentNotification);

            log.info("Successfully sent notification ID: {} of type: {}", updatedNotification.getId(), updatedNotification.getType());

            return new SendNotificationResultImpl(
                    updatedNotification.getId(),
                    updatedNotification.getStatus(),
                    null,
                    true
            );

        } catch (NotificationException e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);

            return new SendNotificationResultImpl(
                    null,
                    NotificationStatus.FAILED,
                    e.getMessage(),
                    false
            );

        } catch (Exception e) {
            log.error("Unexpected error while sending notification: {}", e.getMessage(), e);

            return new SendNotificationResultImpl(
                    null,
                    NotificationStatus.FAILED,
                    "Internal server error: " + e.getMessage(),
                    false
            );
        }
    }

    /**
     * Implementation of SendNotificationResult.
     */
    private static class SendNotificationResultImpl implements SendNotificationResult {

        private final String notificationId;
        private final NotificationStatus status;
        private final String errorMessage;
        private final boolean success;

        public SendNotificationResultImpl(String notificationId, NotificationStatus status,
                                        String errorMessage, boolean success) {
            this.notificationId = notificationId;
            this.status = status;
            this.errorMessage = errorMessage;
            this.success = success;
        }

        @Override
        public String getNotificationId() {
            return notificationId;
        }

        @Override
        public NotificationStatus getStatus() {
            return status;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public boolean isSuccess() {
            return success;
        }
    }
}