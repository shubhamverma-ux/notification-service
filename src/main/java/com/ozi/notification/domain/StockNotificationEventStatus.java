package com.ozi.notification.domain;

/**
 * Status enum for stock notification events.
 */
public enum StockNotificationEventStatus {
    /**
     * Event received from SQS, awaiting processing
     */
    PENDING,

    /**
     * Event is currently being processed
     */
    PROCESSING,

    /**
     * Event successfully sent to CleverTap
     */
    SENT,

    /**
     * Event processing failed
     */
    FAILED,

    /**
     * Event skipped (duplicate or filtered out)
     */
    SKIPPED
}
