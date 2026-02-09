package com.ozi.notification.infrastructure.repository;

import com.ozi.notification.domain.NotificationStatus;
import com.ozi.notification.infrastructure.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA repository for notification entities.
 */
@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {

    /**
     * Find notifications by status.
     */
    List<NotificationEntity> findByStatus(NotificationStatus status);

    /**
     * Find notifications by recipient.
     */
    List<NotificationEntity> findByRecipient(String recipient);

    /**
     * Find pending notifications that are ready to be processed.
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.status = 'PENDING' ORDER BY n.createdAt ASC")
    List<NotificationEntity> findPendingNotifications();

    /**
     * Update notification status.
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.status = :status WHERE n.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") NotificationStatus status);

    /**
     * Update notification status with error message.
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.status = :status, n.errorMessage = :errorMessage WHERE n.id = :id")
    int updateStatusWithError(@Param("id") String id, @Param("status") NotificationStatus status, @Param("errorMessage") String errorMessage);

    /**
     * Update notification as sent.
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.status = 'SENT', n.sentAt = :sentAt WHERE n.id = :id")
    int markAsSent(@Param("id") String id, @Param("sentAt") LocalDateTime sentAt);

    /**
     * Update notification as failed.
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.status = 'FAILED', n.errorMessage = :errorMessage WHERE n.id = :id")
    int markAsFailed(@Param("id") String id, @Param("errorMessage") String errorMessage);
}