package com.ozi.notification.infrastructure.repository;

import com.ozi.notification.domain.Notification;
import com.ozi.notification.domain.NotificationRepository;
import com.ozi.notification.domain.NotificationStatus;
import com.ozi.notification.infrastructure.entity.NotificationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationRepository using JPA.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(Notification notification) {
        log.debug("Saving notification with ID: {}", notification.getId());

        NotificationEntity entity = NotificationEntity.fromDomain(notification);
        NotificationEntity savedEntity = jpaRepository.save(entity);

        log.debug("Successfully saved notification with ID: {}", savedEntity.getId());
        return savedEntity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> findById(String id) {
        log.debug("Finding notification by ID: {}", id);

        return jpaRepository.findById(id)
                .map(NotificationEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByStatus(NotificationStatus status) {
        log.debug("Finding notifications by status: {}", status);

        return jpaRepository.findByStatus(status)
                .stream()
                .map(NotificationEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByRecipient(String recipient) {
        log.debug("Finding notifications by recipient: {}", recipient);

        return jpaRepository.findByRecipient(recipient)
                .stream()
                .map(NotificationEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findPendingNotifications() {
        log.debug("Finding pending notifications");

        return jpaRepository.findPendingNotifications()
                .stream()
                .map(NotificationEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateStatus(String id, NotificationStatus status) {
        log.debug("Updating notification {} status to {}", id, status);

        int updatedRows = jpaRepository.updateStatus(id, status);
        boolean success = updatedRows > 0;

        if (success) {
            log.debug("Successfully updated notification {} status", id);
        } else {
            log.warn("Failed to update notification {} status - notification not found", id);
        }

        return success;
    }

    @Override
    public boolean updateStatusWithError(String id, NotificationStatus status, String errorMessage) {
        log.debug("Updating notification {} status to {} with error: {}", id, status, errorMessage);

        int updatedRows = jpaRepository.updateStatusWithError(id, status, errorMessage);
        boolean success = updatedRows > 0;

        if (success) {
            log.debug("Successfully updated notification {} status with error", id);
        } else {
            log.warn("Failed to update notification {} status with error - notification not found", id);
        }

        return success;
    }
}