package com.ozi.notification.infrastructure.entity;

import com.ozi.notification.domain.StockNotificationEvent;
import com.ozi.notification.domain.StockNotificationEventStatus;
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
 * JPA entity for stock notification events.
 */
@Entity
@Table(name = "stock_notification_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockNotificationEventEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "sqs_message_id", nullable = false, length = 100)
    private String sqsMessageId;

    @Column(name = "sqs_message_group_id", length = 128)
    private String sqsMessageGroupId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "guest_id", length = 50)
    private String guestId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "screen", length = 50)
    private String screen;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_name", length = 50)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private StockNotificationEventStatus status = StockNotificationEventStatus.PENDING;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Type(JsonType.class)
    @Column(name = "raw_payload", columnDefinition = "json")
    private Map<String, Object> rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (receivedAt == null) {
            receivedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Converts domain StockNotificationEvent to entity.
     */
    public static StockNotificationEventEntity fromDomain(StockNotificationEvent event) {
        return StockNotificationEventEntity.builder()
                .id(event.getId())
                .sqsMessageId(event.getSqsMessageId())
                .sqsMessageGroupId(event.getSqsMessageGroupId())
                .userId(event.getUserId())
                .guestId(event.getGuestId())
                .itemId(event.getItemId())
                .sku(event.getSku())
                .screen(event.getScreen())
                .sourceType(event.getSourceType())
                .sourceName(event.getSourceName())
                .status(event.getStatus())
                .receivedAt(event.getReceivedAt())
                .processedAt(event.getProcessedAt())
                .sentAt(event.getSentAt())
                .errorMessage(event.getErrorMessage())
                .retryCount(event.getRetryCount())
                .rawPayload(event.getRawPayload())
                .build();
    }

    /**
     * Converts entity to domain StockNotificationEvent.
     */
    public StockNotificationEvent toDomain() {
        return StockNotificationEvent.builder()
                .id(this.id)
                .sqsMessageId(this.sqsMessageId)
                .sqsMessageGroupId(this.sqsMessageGroupId)
                .userId(this.userId)
                .guestId(this.guestId)
                .itemId(this.itemId)
                .sku(this.sku)
                .screen(this.screen)
                .sourceType(this.sourceType)
                .sourceName(this.sourceName)
                .status(this.status)
                .receivedAt(this.receivedAt)
                .processedAt(this.processedAt)
                .sentAt(this.sentAt)
                .errorMessage(this.errorMessage)
                .retryCount(this.retryCount != null ? this.retryCount : 0)
                .rawPayload(this.rawPayload)
                .build();
    }
}
