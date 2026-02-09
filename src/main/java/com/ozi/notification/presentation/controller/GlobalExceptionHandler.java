package com.ozi.notification.presentation.controller;

import com.ozi.notification.application.dto.SendNotificationResponseDto;
import com.ozi.notification.domain.NotificationException;
import com.ozi.notification.domain.NotificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SendNotificationResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String errorMessage = "Validation failed: " + errors.toString();

        SendNotificationResponseDto response = SendNotificationResponseDto.builder()
                .status(NotificationStatus.FAILED)
                .errorMessage(errorMessage)
                .success(false)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle notification domain exceptions.
     */
    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<SendNotificationResponseDto> handleNotificationException(NotificationException ex) {
        log.error("Notification exception: {}", ex.getMessage(), ex);

        SendNotificationResponseDto response = SendNotificationResponseDto.builder()
                .notificationId(ex.getNotificationId())
                .status(NotificationStatus.FAILED)
                .errorMessage(ex.getMessage())
                .success(false)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SendNotificationResponseDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        SendNotificationResponseDto response = SendNotificationResponseDto.builder()
                .status(NotificationStatus.FAILED)
                .errorMessage("Invalid request: " + ex.getMessage())
                .success(false)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<SendNotificationResponseDto> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        SendNotificationResponseDto response = SendNotificationResponseDto.builder()
                .status(NotificationStatus.FAILED)
                .errorMessage("Internal server error")
                .success(false)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}