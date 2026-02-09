package com.ozi.notification.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for stock notification event persistence operations.
 */
public interface StockNotificationEventRepository {

    /**
     * Saves a stock notification event.
     *
     * @param event The event to save
     * @return The saved event
     */
    StockNotificationEvent save(StockNotificationEvent event);

    /**
     * Saves multiple stock notification events.
     *
     * @param events The events to save
     * @return The saved events
     */
    List<StockNotificationEvent> saveAll(List<StockNotificationEvent> events);

    /**
     * Finds an event by its ID.
     *
     * @param id The event ID
     * @return Optional containing the event if found
     */
    Optional<StockNotificationEvent> findById(String id);

    /**
     * Finds events by status.
     *
     * @param status The event status
     * @return List of events with the given status
     */
    List<StockNotificationEvent> findByStatus(StockNotificationEventStatus status);

    /**
     * Finds pending events that are ready to be processed.
     *
     * @return List of pending events
     */
    List<StockNotificationEvent> findPendingEvents();

    /**
     * Finds pending events for a specific date (for daily processing).
     *
     * @param date The date to filter by
     * @return List of pending events received on that date
     */
    List<StockNotificationEvent> findPendingEventsByDate(LocalDate date);

    /**
     * Checks if a notification was already sent to a user for a specific SKU on a given date.
     * Used for deduplication (one notification per user per SKU per day).
     *
     * @param userId The user ID
     * @param sku    The product SKU
     * @param date   The date to check
     * @return true if a notification was already sent
     */
    boolean existsSentNotificationForUserSkuOnDate(String userId, String sku, LocalDate date);

    /**
     * Finds distinct user-SKU combinations that haven't been notified today.
     * Returns only the first event for each user-SKU combination.
     *
     * @param date The date to process
     * @return List of unique events (one per user-SKU combination)
     */
    List<StockNotificationEvent> findDistinctPendingEventsForDate(LocalDate date);

    /**
     * Updates the status of an event.
     *
     * @param id     The event ID
     * @param status The new status
     * @return true if the update was successful
     */
    boolean updateStatus(String id, StockNotificationEventStatus status);

    /**
     * Updates the status of an event with an error message.
     *
     * @param id           The event ID
     * @param status       The new status
     * @param errorMessage The error message
     * @return true if the update was successful
     */
    boolean updateStatusWithError(String id, StockNotificationEventStatus status, String errorMessage);

    /**
     * Marks duplicate events as skipped for a user-SKU combination on a date.
     *
     * @param userId     The user ID
     * @param sku        The product SKU
     * @param date       The date
     * @param excludeId  The event ID to exclude (the one being processed)
     * @return Number of events marked as skipped
     */
    int markDuplicatesAsSkipped(String userId, String sku, LocalDate date, String excludeId);
}
