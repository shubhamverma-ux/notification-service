package com.ozi.notification.presentation.controller;

import com.ozi.notification.domain.ProcessStockNotificationsUseCase;
import com.ozi.notification.domain.StockNotificationEvent;
import com.ozi.notification.domain.StockNotificationEventRepository;
import com.ozi.notification.domain.StockNotificationEventStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for stock notification operations.
 */
@RestController
@RequestMapping("/stock-notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stock Notification API", description = "API for managing back-in-stock notifications")
public class StockNotificationController {

    private final ProcessStockNotificationsUseCase processStockNotificationsUseCase;
    private final StockNotificationEventRepository eventRepository;

    /**
     * Process pending stock notification events and send to CleverTap.
     * This endpoint should be called via cron at 10 AM IST daily.
     */
    @PostMapping("/process")
    @Operation(
            summary = "Process stock notifications",
            description = "Process pending stock notification events for today and send to CleverTap. " +
                    "Applies deduplication (one notification per user per SKU per day)."
    )
    public ResponseEntity<ProcessStockNotificationsResponse> processStockNotifications(
            @Parameter(description = "Date to process (defaults to today). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate processDate = date != null ? date : LocalDate.now();
        log.info("Received request to process stock notifications for date: {}", processDate);

        try {
            ProcessStockNotificationsUseCase.ProcessStockNotificationsResult result =
                    processStockNotificationsUseCase.execute(processDate);

            ProcessStockNotificationsResponse response = ProcessStockNotificationsResponse.builder()
                    .date(processDate.toString())
                    .totalEvents(result.getTotalEvents())
                    .totalProcessed(result.getTotalProcessed())
                    .totalSent(result.getTotalSent())
                    .totalFailed(result.getTotalFailed())
                    .totalSkipped(result.getTotalSkipped())
                    .failedEventIds(result.getFailedEventIds())
                    .errorMessages(result.getErrorMessages())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .success(result.getTotalFailed() == 0)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing stock notifications: {}", e.getMessage(), e);

            ProcessStockNotificationsResponse errorResponse = ProcessStockNotificationsResponse.builder()
                    .date(processDate.toString())
                    .totalEvents(0)
                    .totalProcessed(0)
                    .totalSent(0)
                    .totalFailed(0)
                    .totalSkipped(0)
                    .errorMessages(List.of("Internal server error: " + e.getMessage()))
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .success(false)
                    .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get stock notification events by status.
     */
    @GetMapping("/events/status/{status}")
    @Operation(
            summary = "Get events by status",
            description = "Retrieve stock notification events filtered by status"
    )
    public ResponseEntity<List<StockNotificationEventDto>> getEventsByStatus(
            @PathVariable String status
    ) {
        log.info("Received request to get stock notification events by status: {}", status);

        try {
            StockNotificationEventStatus eventStatus = StockNotificationEventStatus.valueOf(status.toUpperCase());
            List<StockNotificationEventDto> events = eventRepository.findByStatus(eventStatus)
                    .stream()
                    .map(StockNotificationEventDto::fromDomain)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(events);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status provided: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get stock notification event by ID.
     */
    @GetMapping("/events/{id}")
    @Operation(
            summary = "Get event by ID",
            description = "Retrieve a stock notification event by its unique identifier"
    )
    public ResponseEntity<StockNotificationEventDto> getEventById(@PathVariable String id) {
        log.info("Received request to get stock notification event by ID: {}", id);

        return eventRepository.findById(id)
                .map(StockNotificationEventDto::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get pending events count for a date.
     */
    @GetMapping("/events/pending/count")
    @Operation(
            summary = "Get pending events count",
            description = "Get the count of pending stock notification events for a specific date"
    )
    public ResponseEntity<PendingEventsCountResponse> getPendingEventsCount(
            @Parameter(description = "Date to check (defaults to today). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate checkDate = date != null ? date : LocalDate.now();
        log.info("Received request to get pending events count for date: {}", checkDate);

        List<StockNotificationEvent> pendingEvents = eventRepository.findPendingEventsByDate(checkDate);
        List<StockNotificationEvent> distinctEvents = eventRepository.findDistinctPendingEventsForDate(checkDate);

        PendingEventsCountResponse response = PendingEventsCountResponse.builder()
                .date(checkDate.toString())
                .totalPendingEvents(pendingEvents.size())
                .distinctUserSkuCombinations(distinctEvents.size())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Response DTO for processing stock notifications.
     */
    @Value
    @Builder
    public static class ProcessStockNotificationsResponse {
        String date;
        int totalEvents;
        int totalProcessed;
        int totalSent;
        int totalFailed;
        int totalSkipped;
        List<String> failedEventIds;
        List<String> errorMessages;
        String timestamp;
        boolean success;
    }

    /**
     * Response DTO for pending events count.
     */
    @Value
    @Builder
    public static class PendingEventsCountResponse {
        String date;
        int totalPendingEvents;
        int distinctUserSkuCombinations;
        String timestamp;
    }

    /**
     * DTO for stock notification event.
     */
    @Value
    @Builder
    public static class StockNotificationEventDto {
        String id;
        String sqsMessageId;
        String userId;
        String guestId;
        Long itemId;
        String sku;
        String screen;
        String sourceType;
        String sourceName;
        String status;
        String receivedAt;
        String processedAt;
        String sentAt;
        String errorMessage;
        int retryCount;

        public static StockNotificationEventDto fromDomain(StockNotificationEvent event) {
            return StockNotificationEventDto.builder()
                    .id(event.getId())
                    .sqsMessageId(event.getSqsMessageId())
                    .userId(event.getUserId())
                    .guestId(event.getGuestId())
                    .itemId(event.getItemId())
                    .sku(event.getSku())
                    .screen(event.getScreen())
                    .sourceType(event.getSourceType())
                    .sourceName(event.getSourceName())
                    .status(event.getStatus().name())
                    .receivedAt(event.getReceivedAt() != null ?
                            event.getReceivedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                    .processedAt(event.getProcessedAt() != null ?
                            event.getProcessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                    .sentAt(event.getSentAt() != null ?
                            event.getSentAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                    .errorMessage(event.getErrorMessage())
                    .retryCount(event.getRetryCount())
                    .build();
        }
    }
}
