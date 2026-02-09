package com.ozi.notification.infrastructure.repository;

import com.ozi.notification.domain.StockNotificationEvent;
import com.ozi.notification.domain.StockNotificationEventRepository;
import com.ozi.notification.domain.StockNotificationEventStatus;
import com.ozi.notification.infrastructure.entity.StockNotificationEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of StockNotificationEventRepository using JPA.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockNotificationEventRepositoryImpl implements StockNotificationEventRepository {

    private final StockNotificationEventJpaRepository jpaRepository;

    @Override
    public StockNotificationEvent save(StockNotificationEvent event) {
        log.debug("Saving stock notification event with ID: {}", event.getId());

        StockNotificationEventEntity entity = StockNotificationEventEntity.fromDomain(event);
        StockNotificationEventEntity savedEntity = jpaRepository.save(entity);

        log.debug("Successfully saved stock notification event with ID: {}", savedEntity.getId());
        return savedEntity.toDomain();
    }

    @Override
    public List<StockNotificationEvent> saveAll(List<StockNotificationEvent> events) {
        log.debug("Saving {} stock notification events", events.size());

        List<StockNotificationEventEntity> entities = events.stream()
                .map(StockNotificationEventEntity::fromDomain)
                .collect(Collectors.toList());

        List<StockNotificationEventEntity> savedEntities = jpaRepository.saveAll(entities);

        log.debug("Successfully saved {} stock notification events", savedEntities.size());
        return savedEntities.stream()
                .map(StockNotificationEventEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockNotificationEvent> findById(String id) {
        log.debug("Finding stock notification event by ID: {}", id);

        return jpaRepository.findById(id)
                .map(StockNotificationEventEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockNotificationEvent> findByStatus(StockNotificationEventStatus status) {
        log.debug("Finding stock notification events by status: {}", status);

        return jpaRepository.findByStatus(status)
                .stream()
                .map(StockNotificationEventEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockNotificationEvent> findPendingEvents() {
        log.debug("Finding pending stock notification events");

        return jpaRepository.findPendingEvents()
                .stream()
                .map(StockNotificationEventEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockNotificationEvent> findPendingEventsByDate(LocalDate date) {
        log.debug("Finding pending stock notification events for date: {}", date);

        return jpaRepository.findPendingEventsByDate(date)
                .stream()
                .map(StockNotificationEventEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsSentNotificationForUserSkuOnDate(String userId, String sku, LocalDate date) {
        log.debug("Checking if notification exists for user: {}, sku: {}, date: {}", userId, sku, date);

        return jpaRepository.existsSentNotificationForUserSkuOnDate(userId, sku, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockNotificationEvent> findDistinctPendingEventsForDate(LocalDate date) {
        log.debug("Finding distinct pending events for date: {}", date);

        return jpaRepository.findDistinctPendingEventsForDate(date)
                .stream()
                .map(StockNotificationEventEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateStatus(String id, StockNotificationEventStatus status) {
        log.debug("Updating stock notification event {} status to {}", id, status);

        int updatedRows = jpaRepository.updateStatus(id, status, LocalDateTime.now());
        boolean success = updatedRows > 0;

        if (success) {
            log.debug("Successfully updated stock notification event {} status", id);
        } else {
            log.warn("Failed to update stock notification event {} status - event not found", id);
        }

        return success;
    }

    @Override
    public boolean updateStatusWithError(String id, StockNotificationEventStatus status, String errorMessage) {
        log.debug("Updating stock notification event {} status to {} with error: {}", id, status, errorMessage);

        int updatedRows = jpaRepository.updateStatusWithError(id, status, errorMessage, LocalDateTime.now());
        boolean success = updatedRows > 0;

        if (success) {
            log.debug("Successfully updated stock notification event {} status with error", id);
        } else {
            log.warn("Failed to update stock notification event {} status with error - event not found", id);
        }

        return success;
    }

    @Override
    public int markDuplicatesAsSkipped(String userId, String sku, LocalDate date, String excludeId) {
        log.debug("Marking duplicates as skipped for user: {}, sku: {}, date: {}, excluding: {}",
                userId, sku, date, excludeId);

        int skippedCount = jpaRepository.markDuplicatesAsSkipped(userId, sku, date, excludeId, LocalDateTime.now());

        log.debug("Marked {} duplicate events as skipped", skippedCount);
        return skippedCount;
    }
}
