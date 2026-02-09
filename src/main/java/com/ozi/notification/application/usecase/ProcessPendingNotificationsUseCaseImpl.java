package com.ozi.notification.application.usecase;

import com.ozi.notification.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the ProcessPendingNotificationsUseCase.
 * This handles batch processing of pending notifications from the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessPendingNotificationsUseCaseImpl implements ProcessPendingNotificationsUseCase {

    private final NotificationRepository notificationRepository;
    private final NotificationServiceProvider notificationServiceProvider;

    @Override
    public ProcessPendingNotificationsResult execute() {
        log.info("Starting batch processing of pending notifications");

        List<Notification> pendingNotifications = notificationRepository.findByStatus(NotificationStatus.PENDING);

        if (pendingNotifications.isEmpty()) {
            log.info("No pending notifications found");
            return new ProcessPendingNotificationsResultImpl(0, 0, 0, List.of(), List.of());
        }

        log.info("Found {} pending notifications to process", pendingNotifications.size());

        int totalProcessed = 0;
        int totalSuccessful = 0;
        int totalFailed = 0;
        List<String> failedNotificationIds = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (Notification notification : pendingNotifications) {
            try {
                log.debug("Processing notification ID: {}", notification.getId());

                // Update status to processing
                notificationRepository.updateStatus(notification.getId(), NotificationStatus.PROCESSING);

                // Send the notification
                Notification sentNotification = notificationServiceProvider.sendNotification(notification);

                // Save the result
                notificationRepository.save(sentNotification);

                totalSuccessful++;
                log.debug("Successfully processed notification ID: {}", notification.getId());

            } catch (NotificationException e) {
                log.error("Failed to send notification ID: {}: {}", notification.getId(), e.getMessage());

                // Update status to failed
                notificationRepository.updateStatusWithError(
                    notification.getId(),
                    NotificationStatus.FAILED,
                    e.getMessage()
                );

                totalFailed++;
                failedNotificationIds.add(notification.getId());
                errorMessages.add(e.getMessage());

            } catch (Exception e) {
                log.error("Unexpected error processing notification ID: {}: {}", notification.getId(), e.getMessage(), e);

                String errorMsg = "Unexpected error: " + e.getMessage();
                notificationRepository.updateStatusWithError(
                    notification.getId(),
                    NotificationStatus.FAILED,
                    errorMsg
                );

                totalFailed++;
                failedNotificationIds.add(notification.getId());
                errorMessages.add(errorMsg);
            }

            totalProcessed++;
        }

        log.info("Completed batch processing. Processed: {}, Successful: {}, Failed: {}",
                totalProcessed, totalSuccessful, totalFailed);

        return new ProcessPendingNotificationsResultImpl(
                totalProcessed,
                totalSuccessful,
                totalFailed,
                failedNotificationIds,
                errorMessages
        );
    }

    /**
     * Implementation of ProcessPendingNotificationsResult.
     */
    private static class ProcessPendingNotificationsResultImpl implements ProcessPendingNotificationsResult {

        private final int totalProcessed;
        private final int totalSuccessful;
        private final int totalFailed;
        private final List<String> failedNotificationIds;
        private final List<String> errorMessages;

        public ProcessPendingNotificationsResultImpl(int totalProcessed, int totalSuccessful,
                                                   int totalFailed, List<String> failedNotificationIds,
                                                   List<String> errorMessages) {
            this.totalProcessed = totalProcessed;
            this.totalSuccessful = totalSuccessful;
            this.totalFailed = totalFailed;
            this.failedNotificationIds = failedNotificationIds;
            this.errorMessages = errorMessages;
        }

        @Override
        public int getTotalProcessed() {
            return totalProcessed;
        }

        @Override
        public int getTotalSuccessful() {
            return totalSuccessful;
        }

        @Override
        public int getTotalFailed() {
            return totalFailed;
        }

        @Override
        public List<String> getFailedNotificationIds() {
            return failedNotificationIds;
        }

        @Override
        public List<String> getErrorMessages() {
            return errorMessages;
        }
    }
}