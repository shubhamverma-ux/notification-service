package com.ozi.notification.infrastructure.repository;

import com.ozi.notification.domain.StockNotificationEventStatus;
import com.ozi.notification.infrastructure.entity.StockNotificationEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA repository for stock notification event entities.
 */
@Repository
public interface StockNotificationEventJpaRepository extends JpaRepository<StockNotificationEventEntity, String> {

    /**
     * Find events by status.
     */
    List<StockNotificationEventEntity> findByStatus(StockNotificationEventStatus status);

    /**
     * Find pending events ordered by received time.
     */
    @Query("SELECT e FROM StockNotificationEventEntity e WHERE e.status = 'PENDING' ORDER BY e.receivedAt ASC")
    List<StockNotificationEventEntity> findPendingEvents();

    /**
     * Find pending events received on a specific date.
     */
    @Query("SELECT e FROM StockNotificationEventEntity e WHERE e.status = 'PENDING' " +
           "AND DATE(e.receivedAt) = :date ORDER BY e.receivedAt ASC")
    List<StockNotificationEventEntity> findPendingEventsByDate(@Param("date") LocalDate date);

    /**
     * Check if a notification was already sent to a user for a specific SKU on a given date.
     */
    @Query("SELECT COUNT(e) > 0 FROM StockNotificationEventEntity e " +
           "WHERE e.userId = :userId AND e.sku = :sku " +
           "AND DATE(e.receivedAt) = :date AND e.status = 'SENT'")
    boolean existsSentNotificationForUserSkuOnDate(
            @Param("userId") String userId,
            @Param("sku") String sku,
            @Param("date") LocalDate date);

    /**
     * Find distinct pending events for a date (one per user-SKU combination).
     * Uses a subquery to get the minimum ID for each user-SKU combination.
     */
    @Query(value = "SELECT e.* FROM stock_notification_events e " +
           "INNER JOIN (" +
           "    SELECT user_id, sku, MIN(id) as min_id " +
           "    FROM stock_notification_events " +
           "    WHERE status = 'PENDING' AND DATE(received_at) = :date " +
           "    GROUP BY user_id, sku" +
           ") grouped ON e.id = grouped.min_id " +
           "ORDER BY e.received_at ASC",
           nativeQuery = true)
    List<StockNotificationEventEntity> findDistinctPendingEventsForDate(@Param("date") LocalDate date);

    /**
     * Update event status.
     */
    @Modifying
    @Query("UPDATE StockNotificationEventEntity e SET e.status = :status, e.updatedAt = :now WHERE e.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") StockNotificationEventStatus status, @Param("now") LocalDateTime now);

    /**
     * Update event status with error message.
     */
    @Modifying
    @Query("UPDATE StockNotificationEventEntity e SET e.status = :status, e.errorMessage = :errorMessage, " +
           "e.processedAt = :now, e.updatedAt = :now WHERE e.id = :id")
    int updateStatusWithError(
            @Param("id") String id,
            @Param("status") StockNotificationEventStatus status,
            @Param("errorMessage") String errorMessage,
            @Param("now") LocalDateTime now);

    /**
     * Mark event as sent.
     */
    @Modifying
    @Query("UPDATE StockNotificationEventEntity e SET e.status = 'SENT', e.sentAt = :now, " +
           "e.processedAt = :now, e.updatedAt = :now, e.errorMessage = null WHERE e.id = :id")
    int markAsSent(@Param("id") String id, @Param("now") LocalDateTime now);

    /**
     * Mark duplicate events as skipped for a user-SKU combination on a date.
     */
    @Modifying
    @Query("UPDATE StockNotificationEventEntity e SET e.status = 'SKIPPED', " +
           "e.errorMessage = 'Duplicate: another event for same user-SKU was processed', " +
           "e.processedAt = :now, e.updatedAt = :now " +
           "WHERE e.userId = :userId AND e.sku = :sku AND DATE(e.receivedAt) = :date " +
           "AND e.id != :excludeId AND e.status = 'PENDING'")
    int markDuplicatesAsSkipped(
            @Param("userId") String userId,
            @Param("sku") String sku,
            @Param("date") LocalDate date,
            @Param("excludeId") String excludeId,
            @Param("now") LocalDateTime now);
}
