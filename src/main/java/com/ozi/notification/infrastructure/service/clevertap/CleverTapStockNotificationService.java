package com.ozi.notification.infrastructure.service.clevertap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozi.notification.domain.NotificationException;
import com.ozi.notification.domain.NotificationType;
import com.ozi.notification.domain.StockNotificationEvent;
import com.ozi.notification.domain.StockNotificationService;
import com.ozi.notification.infrastructure.config.StockNotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * CleverTap implementation for sending stock notifications.
 * Uses the CleverTap Upload Events API to raise stock_status_changed events,
 * which triggers the live behavior campaign in CleverTap.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CleverTapStockNotificationService implements StockNotificationService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final StockNotificationProperties stockNotificationProperties;

    @Value("${notification.clevertap.account-id}")
    private String accountId;

    @Value("${notification.clevertap.passcode}")
    private String passcode;

    @Value("${notification.clevertap.base-url}")
    private String baseUrl;

    @Override
    public boolean sendStockNotification(StockNotificationEvent event) throws NotificationException {
        String recipientId = event.getEffectiveRecipientId();
        if (recipientId == null || recipientId.isBlank()) {
            throw new NotificationException(
                    "No valid recipient ID for stock notification",
                    event.getId(),
                    NotificationType.PUSH
            );
        }

        try {
            log.info("Uploading stock_status_changed event to CleverTap: eventId={}, userId={}, sku={}",
                    event.getId(), recipientId, event.getSku());

            // Build event data matching the campaign's expected KVPs
            Map<String, Object> evtData = new HashMap<>();
            evtData.put("notification_type", "BACK_IN_STOCK");
            evtData.put("stock_status", "available");
            evtData.put("productId", String.valueOf(event.getItemId()));
            evtData.put("sku", event.getSku());

            if (event.getScreen() != null) {
                evtData.put("screen", event.getScreen());
            }
            if (event.getSourceType() != null) {
                evtData.put("sourceType", event.getSourceType());
            }
            if (event.getSourceName() != null) {
                evtData.put("sourceName", event.getSourceName());
            }

            // Build the upload event payload
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("identity", recipientId);
            eventPayload.put("type", "event");
            eventPayload.put("evtName", "stock_status_changed");
            eventPayload.put("evtData", evtData);
            eventPayload.put("ts", Instant.now().getEpochSecond());

            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("d", new Object[]{eventPayload});

            String endpoint = baseUrl + "/1/upload";

            log.debug("Sending CleverTap upload event request to: {} with payload: {}",
                    endpoint, objectMapper.writeValueAsString(requestPayload));

            // Send the request
            String responseBody = webClient.post()
                    .uri(endpoint)
                    .header("X-CleverTap-Account-Id", accountId)
                    .header("X-CleverTap-Passcode", passcode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestPayload)
                    .exchangeToMono(response -> {
                        log.debug("CleverTap response status: {}", response.statusCode());
                        return response.bodyToMono(String.class);
                    })
                    .block();
            log.debug("CleverTap response: {}", responseBody);

            // Parse response
            JsonNode responseJson = objectMapper.readTree(responseBody);

            // Check for processed count
            if (responseJson.has("processed")) {
                int processed = responseJson.get("processed").asInt();
                int unprocessed = responseJson.has("unprocessed") ? responseJson.get("unprocessed").asInt() : 0;

                if (processed > 0) {
                    log.info("Successfully uploaded stock_status_changed event to CleverTap: eventId={}, userId={}, processed={}",
                            event.getId(), recipientId, processed);
                    return true;
                } else {
                    String errorMsg = "CleverTap processed 0 events, unprocessed: " + unprocessed + ", response: " + responseBody;
                    log.error(errorMsg);
                    throw new NotificationException(errorMsg, event.getId(), NotificationType.PUSH);
                }
            }

            if (responseJson.has("status") && "success".equals(responseJson.get("status").asText())) {
                log.info("Successfully uploaded stock_status_changed event to CleverTap: eventId={}, userId={}",
                        event.getId(), recipientId);
                return true;
            }

            if (responseJson.has("error")) {
                String errorMessage = responseJson.get("error").asText();
                log.error("CleverTap API error for event {}: {}", event.getId(), errorMessage);
                throw new NotificationException(
                        "CleverTap API error: " + errorMessage,
                        event.getId(),
                        NotificationType.PUSH
                );
            }

            String errorMsg = "Unknown CleverTap response: " + responseBody;
            log.error(errorMsg);
            throw new NotificationException(errorMsg, event.getId(), NotificationType.PUSH);

        } catch (NotificationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload stock notification event to CleverTap: eventId={}, error={}",
                    event.getId(), e.getMessage(), e);
            throw new NotificationException(
                    "Failed to send stock notification: " + e.getMessage(),
                    event.getId(),
                    NotificationType.PUSH,
                    e
            );
        }
    }
}
