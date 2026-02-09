package com.ozi.notification.infrastructure.entity;

import com.ozi.notification.domain.Notification;
import com.ozi.notification.domain.NotificationPriority;
import com.ozi.notification.domain.NotificationStatus;
import com.ozi.notification.domain.NotificationType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JPA entity for notifications.
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Type(JsonType.class)
    @Column(name = "data", columnDefinition = "json")
    private Map<String, String> data;

    @Column(name = "deep_link", columnDefinition = "TEXT")
    private String deepLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata;

    /**
     * Converts domain Notification to NotificationEntity.
     */
    public static NotificationEntity fromDomain(Notification notification) {
        return NotificationEntity.builder()
                .id(notification.getId())
                .type(notification.getType())
                .recipient(notification.getRecipient())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getData())
                .deepLink(notification.getDeepLink())
                .priority(notification.getPriority())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .errorMessage(notification.getErrorMessage())
                .metadata(notification.getMetadata())
                .build();
    }

    /**
     * Converts NotificationEntity to domain Notification.
     */
    public Notification toDomain() {
        return Notification.builder()
                .id(this.id)
                .type(this.type)
                .recipient(this.recipient)
                .title(this.title)
                .message(this.message)
                .data(this.data)
                .deepLink(this.deepLink)
                .priority(this.priority)
                .status(this.status)
                .createdAt(this.createdAt)
                .sentAt(this.sentAt)
                .errorMessage(this.errorMessage)
                .metadata(this.metadata)
                .build();
    }
}