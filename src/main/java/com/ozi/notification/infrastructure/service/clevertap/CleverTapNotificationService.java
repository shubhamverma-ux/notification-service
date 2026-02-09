package com.ozi.notification.infrastructure.service.clevertap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozi.notification.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * CleverTap notification service implementation.
 * Sends push notifications via CleverTap API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CleverTapNotificationService implements NotificationService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${notification.clevertap.account-id}")
    private String accountId;

    @Value("${notification.clevertap.passcode}")
    private String passcode;

    @Value("${notification.clevertap.base-url}")
    private String baseUrl;

    @Override
    public Notification sendNotification(Notification notification) throws NotificationException {
        if (!canHandle(notification.getType())) {
            throw new NotificationException(
                "CleverTap service cannot handle notification type: " + notification.getType(),
                notification.getId(),
                notification.getType()
            );
        }

        try {
            log.info("Sending CleverTap notification to recipient: {}", notification.getRecipient());

            // Prepare key-value pairs for the notification
            Map<String, Object> kvs = new HashMap<>();
            kvs.put("wzrk_title", notification.getTitle());
            kvs.put("wzrk_body", notification.getMessage());

            // Add deep link if provided
            if (notification.getDeepLink() != null && !notification.getDeepLink().isEmpty()) {
                kvs.put("wzrk_dl", notification.getDeepLink());
            }

            // Add custom data
            if (notification.getData() != null) {
                notification.getData().forEach((key, value) -> {
                    if (value != null) {
                        kvs.put(key, value);
                    }
                });
            }

            // Prepare the request payload
            Map<String, Object> externalTrigger = new HashMap<>();
            Map<String, Object> to = new HashMap<>();
            to.put("Identity", new String[]{notification.getRecipient()});
            externalTrigger.put("to", to);
            externalTrigger.put("kvs", kvs);

            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("ExternalTrigger", new Object[]{externalTrigger});

            // Determine the endpoint
            String endpoint = baseUrl + "/1/send/externaltrigger.json";

            log.debug("Sending request to CleverTap: {} with payload: {}",
                     endpoint, objectMapper.writeValueAsString(requestPayload));

            // Send the request
            Mono<String> responseMono = webClient.post()
                    .uri(endpoint)
                    .header("X-CleverTap-Account-Id", accountId)
                    .header("X-CleverTap-Passcode", passcode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestPayload)
                    .retrieve()
                    .bodyToMono(String.class);

            String responseBody = responseMono.block();
            log.debug("CleverTap response: {}", responseBody);

            // Parse response
            JsonNode responseJson = objectMapper.readTree(responseBody);

            if (responseJson.has("error")) {
                String errorMessage = responseJson.get("error").asText();
                log.error("CleverTap API error: {}", errorMessage);
                throw new NotificationException(
                    "CleverTap API error: " + errorMessage,
                    notification.getId(),
                    notification.getType()
                );
            }

            // Check for success status
            if (responseJson.has("status") && "success".equals(responseJson.get("status").asText())) {
                log.info("Successfully sent CleverTap notification: {}", notification.getId());
                return notification.markAsSent();
            } else {
                String errorMsg = "Unknown CleverTap response: " + responseBody;
                log.error(errorMsg);
                throw new NotificationException(errorMsg, notification.getId(), notification.getType());
            }

        } catch (Exception e) {
            log.error("Failed to send CleverTap notification: {}", e.getMessage(), e);

            if (e instanceof NotificationException notificationException) {
                throw notificationException;
            }

            throw new NotificationException(
                "Failed to send CleverTap notification: " + e.getMessage(),
                notification.getId(),
                notification.getType(),
                e
            );
        }
    }

    @Override
    public boolean canHandle(NotificationType type) {
        return type == NotificationType.PUSH;
    }
}