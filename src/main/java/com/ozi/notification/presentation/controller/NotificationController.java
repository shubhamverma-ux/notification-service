package com.ozi.notification.presentation.controller;

import com.ozi.notification.application.dto.NotificationDto;
import com.ozi.notification.application.dto.SendNotificationRequestDto;
import com.ozi.notification.application.dto.SendNotificationResponseDto;
import com.ozi.notification.domain.ProcessPendingNotificationsUseCase;
import com.ozi.notification.domain.SendNotificationUseCase;
import com.ozi.notification.domain.NotificationRepository;
import com.ozi.notification.domain.NotificationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for notification operations.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification API", description = "API for sending and managing notifications")
public class NotificationController {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final ProcessPendingNotificationsUseCase processPendingNotificationsUseCase;
    private final NotificationRepository notificationRepository;

    /**
     * Send a notification immediately.
     */
    @PostMapping("/send")
    @Operation(summary = "Send a notification", description = "Send a notification immediately to the specified recipient")
    public ResponseEntity<SendNotificationResponseDto> sendNotification(
            @Valid @RequestBody SendNotificationRequestDto request) {

        log.info("Received send notification request for type: {} to recipient: {}",
                request.getType(), request.getRecipient());

        try {
            SendNotificationUseCase.SendNotificationRequest useCaseRequest =
                new SendNotificationUseCase.SendNotificationRequest() {
                    @Override
                    public com.ozi.notification.domain.NotificationType getType() {
                        return com.ozi.notification.domain.NotificationType.valueOf(request.getType().name());
                    }

                    @Override
                    public String getRecipient() {
                        return request.getRecipient();
                    }

                    @Override
                    public String getTitle() {
                        return request.getTitle();
                    }

                    @Override
                    public String getMessage() {
                        return request.getMessage();
                    }

                    @Override
                    public java.util.Map<String, String> getData() {
                        return request.getData();
                    }

                    @Override
                    public String getDeepLink() {
                        return request.getDeepLink();
                    }

                    @Override
                    public com.ozi.notification.domain.NotificationPriority getPriority() {
                        return com.ozi.notification.domain.NotificationPriority.valueOf(request.getPriority().name());
                    }
                };

            SendNotificationUseCase.SendNotificationResult result = sendNotificationUseCase.execute(useCaseRequest);

            SendNotificationResponseDto response = SendNotificationResponseDto.builder()
                    .notificationId(result.getNotificationId())
                    .status(result.getStatus())
                    .errorMessage(result.getErrorMessage())
                    .success(result.isSuccess())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            HttpStatus status = result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Error processing send notification request: {}", e.getMessage(), e);

            SendNotificationResponseDto errorResponse = SendNotificationResponseDto.builder()
                    .status(NotificationStatus.FAILED)
                    .errorMessage("Internal server error: " + e.getMessage())
                    .success(false)
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get notification by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Retrieve a notification by its unique identifier")
    public ResponseEntity<NotificationDto> getNotification(@PathVariable String id) {
        log.info("Received get notification request for ID: {}", id);

        return notificationRepository.findById(id)
                .map(NotificationDto::fromDomain)
                .map(notification -> ResponseEntity.ok(notification))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get notifications by status.
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get notifications by status", description = "Retrieve notifications filtered by status")
    public ResponseEntity<List<NotificationDto>> getNotificationsByStatus(@PathVariable String status) {
        log.info("Received get notifications by status request: {}", status);

        try {
            NotificationStatus notificationStatus = NotificationStatus.valueOf(status.toUpperCase());
            List<NotificationDto> notifications = notificationRepository.findByStatus(notificationStatus)
                    .stream()
                    .map(NotificationDto::fromDomain)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(notifications);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status provided: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get notifications by recipient.
     */
    @GetMapping("/recipient/{recipient}")
    @Operation(summary = "Get notifications by recipient", description = "Retrieve notifications sent to a specific recipient")
    public ResponseEntity<List<NotificationDto>> getNotificationsByRecipient(@PathVariable String recipient) {
        log.info("Received get notifications by recipient request: {}", recipient);

        List<NotificationDto> notifications = notificationRepository.findByRecipient(recipient)
                .stream()
                .map(NotificationDto::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(notifications);
    }

    /**
     * Process pending notifications.
     */
    @PostMapping("/process-pending")
    @Operation(summary = "Process pending notifications", description = "Process all pending notifications in the queue")
    public ResponseEntity<ProcessPendingNotificationsResponse> processPendingNotifications() {
        log.info("Received process pending notifications request");

        try {
            ProcessPendingNotificationsUseCase.ProcessPendingNotificationsResult result =
                processPendingNotificationsUseCase.execute();

            ProcessPendingNotificationsResponse response = ProcessPendingNotificationsResponse.builder()
                    .totalProcessed(result.getTotalProcessed())
                    .totalSuccessful(result.getTotalSuccessful())
                    .totalFailed(result.getTotalFailed())
                    .failedNotificationIds(result.getFailedNotificationIds())
                    .errorMessages(result.getErrorMessages())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing pending notifications: {}", e.getMessage(), e);

            ProcessPendingNotificationsResponse errorResponse = ProcessPendingNotificationsResponse.builder()
                    .totalProcessed(0)
                    .totalSuccessful(0)
                    .totalFailed(0)
                    .errorMessages(List.of("Internal server error: " + e.getMessage()))
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Response DTO for processing pending notifications.
     */
    @lombok.Value
    @lombok.Builder
    public static class ProcessPendingNotificationsResponse {
        int totalProcessed;
        int totalSuccessful;
        int totalFailed;
        List<String> failedNotificationIds;
        List<String> errorMessages;
        String timestamp;
    }
}