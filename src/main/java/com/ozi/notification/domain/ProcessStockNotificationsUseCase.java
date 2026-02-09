package com.ozi.notification.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Use case interface for processing stock notification events.
 */
public interface ProcessStockNotificationsUseCase {

    /**
     * Processes pending stock notification events for a specific date.
     * Applies deduplication (one notification per user per SKU per day).
     *
     * @param date The date to process events for
     * @return The processing result
     */
    ProcessStockNotificationsResult execute(LocalDate date);

    /**
     * Processes pending stock notification events for today.
     *
     * @return The processing result
     */
    default ProcessStockNotificationsResult execute() {
        return execute(LocalDate.now());
    }

    /**
     * Result of processing stock notifications.
     */
    interface ProcessStockNotificationsResult {
        int getTotalEvents();
        int getTotalProcessed();
        int getTotalSent();
        int getTotalFailed();
        int getTotalSkipped();
        List<String> getFailedEventIds();
        List<String> getErrorMessages();
    }
}
