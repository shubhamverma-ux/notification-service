package com.ozi.notification.domain;

import java.util.List;

/**
 * Use case for processing pending notifications from the database.
 * This represents the business operation of finding and sending queued notifications.
 */
public interface ProcessPendingNotificationsUseCase {

    /**
     * Executes the process pending notifications use case.
     *
     * @return The result containing processing statistics
     */
    ProcessPendingNotificationsResult execute();

    /**
     * Result object for pending notification processing operations.
     */
    interface ProcessPendingNotificationsResult {
        int getTotalProcessed();
        int getTotalSuccessful();
        int getTotalFailed();
        List<String> getFailedNotificationIds();
        List<String> getErrorMessages();
    }
}