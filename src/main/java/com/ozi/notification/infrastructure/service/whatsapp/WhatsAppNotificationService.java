package com.ozi.notification.infrastructure.service.whatsapp;

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
 * WhatsApp notification service implementation.
 * Sends WhatsApp messages via WhatsApp Business API.
 *
 * Note: This is a placeholder implementation. In a real-world scenario,
 * you would integrate with the official WhatsApp Business API or a
 * third-party WhatsApp messaging service like Twilio, 360Dialog, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationService implements NotificationService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${notification.whatsapp.api-url:#{null}}")
    private String apiUrl;

    @Value("${notification.whatsapp.api-key:#{null}}")
    private String apiKey;

    @Override
    public Notification sendNotification(Notification notification) throws NotificationException {
        if (!canHandle(notification.getType())) {
            throw new NotificationException(
                "WhatsApp service cannot handle notification type: " + notification.getType(),
                notification.getId(),
                notification.getType()
            );
        }

        // Validate configuration
        if (apiUrl == null || apiUrl.isEmpty()) {
            throw new NotificationException(
                "WhatsApp API URL not configured",
                notification.getId(),
                notification.getType()
            );
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new NotificationException(
                "WhatsApp API key not configured",
                notification.getId(),
                notification.getType()
            );
        }

        try {
            log.info("Sending WhatsApp message to recipient: {}", notification.getRecipient());

            // Prepare the message payload for WhatsApp Business API
            // This structure may vary depending on the WhatsApp API provider
            Map<String, Object> messagePayload = new HashMap<>();
            messagePayload.put("to", notification.getRecipient());
            messagePayload.put("type", "text");

            Map<String, Object> textContent = new HashMap<>();
            StringBuilder messageText = new StringBuilder();

            if (notification.getTitle() != null && !notification.getTitle().isEmpty()) {
                messageText.append("*").append(notification.getTitle()).append("*\n\n");
            }

            messageText.append(notification.getMessage());

            // Add deep link if provided
            if (notification.getDeepLink() != null && !notification.getDeepLink().isEmpty()) {
                messageText.append("\n\n").append(notification.getDeepLink());
            }

            textContent.put("body", messageText.toString());
            messagePayload.put("text", textContent);

            // Add custom data as metadata if needed
            if (notification.getData() != null && !notification.getData().isEmpty()) {
                // Some APIs allow custom data, this depends on the provider
                messagePayload.put("custom_data", notification.getData());
            }

            log.debug("Sending WhatsApp message payload: {}", objectMapper.writeValueAsString(messagePayload));

            // Send the request (this is a placeholder - actual implementation depends on the WhatsApp API)
            Mono<String> responseMono = webClient.post()
                    .uri(apiUrl + "/messages")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(messagePayload)
                    .retrieve()
                    .bodyToMono(String.class);

            String responseBody = responseMono.block();
            log.debug("WhatsApp API response: {}", responseBody);

            // Parse response (structure depends on the API provider)
            JsonNode responseJson = objectMapper.readTree(responseBody);

            // Check for success (this logic depends on the actual API)
            if (responseJson.has("error")) {
                String errorMessage = responseJson.get("error").get("message").asText();
                log.error("WhatsApp API error: {}", errorMessage);
                throw new NotificationException(
                    "WhatsApp API error: " + errorMessage,
                    notification.getId(),
                    notification.getType()
                );
            }

            // Check for message ID or success indicator
            if (responseJson.has("messages") && responseJson.get("messages").isArray() &&
                responseJson.get("messages").size() > 0) {
                log.info("Successfully sent WhatsApp message: {}", notification.getId());
                return notification.markAsSent();
            } else {
                String errorMsg = "Invalid WhatsApp API response: " + responseBody;
                log.error(errorMsg);
                throw new NotificationException(errorMsg, notification.getId(), notification.getType());
            }

        } catch (Exception e) {
            log.error("Failed to send WhatsApp message: {}", e.getMessage(), e);

            if (e instanceof NotificationException notificationException) {
                throw notificationException;
            }

            throw new NotificationException(
                "Failed to send WhatsApp message: " + e.getMessage(),
                notification.getId(),
                notification.getType(),
                e
            );
        }
    }

    @Override
    public boolean canHandle(NotificationType type) {
        return type == NotificationType.WHATSAPP;
    }
}