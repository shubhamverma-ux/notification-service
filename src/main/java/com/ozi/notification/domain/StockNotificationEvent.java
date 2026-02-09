package com.ozi.notification.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Domain entity representing a stock notification event received from SQS.
 * This represents a "back in stock" notification request for a specific user and product.
 */
@Value
@Builder(toBuilder = true)
public class StockNotificationEvent {

    /**
     * Unique identifier for the event
     */
    String id;

    /**
     * SQS message ID for tracking
     */
    String sqsMessageId;

    /**
     * FIFO queue message group ID
     */
    String sqsMessageGroupId;

    /**
     * User ID to notify (CleverTap Identity)
     */
    String userId;

    /**
     * Guest ID (fallback if userId not available)
     */
    String guestId;

    /**
     * Product item ID
     */
    Long itemId;

    /**
     * Product SKU
     */
    String sku;

    /**
     * Screen for deep link (e.g., "product")
     */
    String screen;

    /**
     * Source type (e.g., "notification")
     */
    String sourceType;

    /**
     * Source name (e.g., "back_in_stock")
     */
    String sourceName;

    /**
     * Current processing status
     */
    StockNotificationEventStatus status;

    /**
     * When the SQS message was received
     */
    LocalDateTime receivedAt;

    /**
     * When processing was attempted
     */
    LocalDateTime processedAt;

    /**
     * When successfully sent to CleverTap
     */
    LocalDateTime sentAt;

    /**
     * Error message if processing failed
     */
    String errorMessage;

    /**
     * Number of processing attempts
     */
    int retryCount;

    /**
     * Original SQS message payload for debugging
     */
    Map<String, Object> rawPayload;

    /**
     * Creates a new stock notification event from SQS message payload.
     */
    public static StockNotificationEvent create(
            String sqsMessageId,
            String sqsMessageGroupId,
            String userId,
            String guestId,
            Long itemId,
            String sku,
            String screen,
            String sourceType,
            String sourceName,
            Map<String, Object> rawPayload
    ) {
        return StockNotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .sqsMessageId(sqsMessageId)
                .sqsMessageGroupId(sqsMessageGroupId)
                .userId(userId)
                .guestId(guestId)
                .itemId(itemId)
                .sku(sku)
                .screen(screen)
                .sourceType(sourceType)
                .sourceName(sourceName)
                .status(StockNotificationEventStatus.PENDING)
                .receivedAt(LocalDateTime.now())
                .retryCount(0)
                .rawPayload(rawPayload)
                .build();
    }

    /**
     * Returns the effective recipient ID (userId takes priority over guestId).
     */
    public String getEffectiveRecipientId() {
        return userId != null && !userId.isBlank() ? userId : guestId;
    }

    /**
     * Marks the event as processing.
     */
    public StockNotificationEvent markAsProcessing() {
        return this.toBuilder()
                .status(StockNotificationEventStatus.PROCESSING)
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Marks the event as sent.
     */
    public StockNotificationEvent markAsSent() {
        return this.toBuilder()
                .status(StockNotificationEventStatus.SENT)
                .sentAt(LocalDateTime.now())
                .errorMessage(null)
                .build();
    }

    /**
     * Marks the event as failed with an error message.
     */
    public StockNotificationEvent markAsFailed(String errorMessage) {
        return this.toBuilder()
                .status(StockNotificationEventStatus.FAILED)
                .errorMessage(errorMessage)
                .retryCount(this.retryCount + 1)
                .build();
    }

    /**
     * Marks the event as skipped (e.g., duplicate).
     */
    public StockNotificationEvent markAsSkipped(String reason) {
        return this.toBuilder()
                .status(StockNotificationEventStatus.SKIPPED)
                .errorMessage(reason)
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Checks if the event is in a terminal state.
     */
    public boolean isTerminal() {
        return status == StockNotificationEventStatus.SENT
                || status == StockNotificationEventStatus.FAILED
                || status == StockNotificationEventStatus.SKIPPED;
    }
}
