package com.ozi.notification.application.usecase;

import com.ozi.notification.domain.*;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ProcessStockNotificationsUseCase.
 * Processes pending stock notification events and sends them to CleverTap.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessStockNotificationsUseCaseImpl implements ProcessStockNotificationsUseCase {

    private final StockNotificationEventRepository eventRepository;
    private final StockNotificationService stockNotificationService;

    @Override
    @Transactional
    public ProcessStockNotificationsResult execute(LocalDate date) {
        log.info("Processing stock notification events for date: {}", date);

        List<String> failedEventIds = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        int totalSent = 0;
        int totalFailed = 0;
        int totalSkipped = 0;

        // Find distinct pending events (one per user-SKU combination)
        List<StockNotificationEvent> events = eventRepository.findDistinctPendingEventsForDate(date);
        int totalEvents = events.size();

        log.info("Found {} distinct pending events to process for date: {}", totalEvents, date);

        for (StockNotificationEvent event : events) {
            try {
                // Check if notification was already sent for this user-SKU today
                boolean alreadySent = eventRepository.existsSentNotificationForUserSkuOnDate(
                        event.getUserId(),
                        event.getSku(),
                        date
                );

                if (alreadySent) {
                    log.debug("Notification already sent for user={}, sku={} on {}. Skipping.",
                            event.getUserId(), event.getSku(), date);
                    eventRepository.updateStatusWithError(
                            event.getId(),
                            StockNotificationEventStatus.SKIPPED,
                            "Duplicate: notification already sent for this user-SKU today"
                    );
                    totalSkipped++;
                    continue;
                }

                // Mark as processing
                eventRepository.updateStatus(event.getId(), StockNotificationEventStatus.PROCESSING);

                // Send notification via CleverTap
                boolean sent = stockNotificationService.sendStockNotification(event);

                if (sent) {
                    // Mark as sent
                    eventRepository.updateStatus(event.getId(), StockNotificationEventStatus.SENT);
                    totalSent++;

                    // Mark any duplicate events for this user-SKU as skipped
                    int skippedDuplicates = eventRepository.markDuplicatesAsSkipped(
                            event.getUserId(),
                            event.getSku(),
                            date,
                            event.getId()
                    );
                    if (skippedDuplicates > 0) {
                        log.debug("Marked {} duplicate events as skipped for user={}, sku={}",
                                skippedDuplicates, event.getUserId(), event.getSku());
                        totalSkipped += skippedDuplicates;
                    }

                    log.info("Successfully processed stock notification: eventId={}, userId={}, sku={}",
                            event.getId(), event.getUserId(), event.getSku());
                } else {
                    // Should not happen as sendStockNotification throws on failure
                    eventRepository.updateStatusWithError(
                            event.getId(),
                            StockNotificationEventStatus.FAILED,
                            "Unknown error: send returned false"
                    );
                    totalFailed++;
                    failedEventIds.add(event.getId());
                }

            } catch (NotificationException e) {
                log.error("Failed to process stock notification event {}: {}",
                        event.getId(), e.getMessage());
                eventRepository.updateStatusWithError(
                        event.getId(),
                        StockNotificationEventStatus.FAILED,
                        e.getMessage()
                );
                totalFailed++;
                failedEventIds.add(event.getId());
                errorMessages.add(String.format("Event %s: %s", event.getId(), e.getMessage()));

            } catch (Exception e) {
                log.error("Unexpected error processing stock notification event {}: {}",
                        event.getId(), e.getMessage(), e);
                eventRepository.updateStatusWithError(
                        event.getId(),
                        StockNotificationEventStatus.FAILED,
                        "Unexpected error: " + e.getMessage()
                );
                totalFailed++;
                failedEventIds.add(event.getId());
                errorMessages.add(String.format("Event %s: %s", event.getId(), e.getMessage()));
            }
        }

        log.info("Completed processing stock notifications for date {}. Total: {}, Sent: {}, Failed: {}, Skipped: {}",
                date, totalEvents, totalSent, totalFailed, totalSkipped);

        return ProcessStockNotificationsResultImpl.builder()
                .totalEvents(totalEvents)
                .totalProcessed(totalSent + totalFailed)
                .totalSent(totalSent)
                .totalFailed(totalFailed)
                .totalSkipped(totalSkipped)
                .failedEventIds(failedEventIds)
                .errorMessages(errorMessages)
                .build();
    }

    @Value
    @Builder
    private static class ProcessStockNotificationsResultImpl implements ProcessStockNotificationsResult {
        int totalEvents;
        int totalProcessed;
        int totalSent;
        int totalFailed;
        int totalSkipped;
        List<String> failedEventIds;
        List<String> errorMessages;
    }
}
