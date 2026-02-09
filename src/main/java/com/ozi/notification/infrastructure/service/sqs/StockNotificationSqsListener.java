package com.ozi.notification.infrastructure.service.sqs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozi.notification.domain.StockNotificationEvent;
import com.ozi.notification.domain.StockNotificationEventRepository;
import com.ozi.notification.infrastructure.config.AwsSqsProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SQS listener service for stock notification events.
 * Polls messages from the SQS FIFO queue and stores them in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(SqsClient.class)
@ConditionalOnProperty(value = "aws.sqs.stock-notification.enabled", havingValue = "true", matchIfMissing = false)
public class StockNotificationSqsListener {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final StockNotificationEventRepository eventRepository;
    private final AwsSqsProperties sqsProperties;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executorService;

    @PostConstruct
    public void start() {
        if (!sqsProperties.getStockNotification().isEnabled()) {
            log.info("Stock notification SQS listener is disabled");
            return;
        }

        String queueUrl = sqsProperties.getStockNotification().getQueueUrl();
        if (queueUrl == null || queueUrl.isBlank()) {
            log.warn("Stock notification SQS queue URL is not configured. Listener will not start.");
            return;
        }

        log.info("Starting stock notification SQS listener for queue: {}", queueUrl);
        running.set(true);
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "sqs-stock-notification-listener");
            thread.setDaemon(true);
            return thread;
        });
        executorService.submit(this::pollMessages);
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping stock notification SQS listener");
        running.set(false);
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void pollMessages() {
        String queueUrl = sqsProperties.getStockNotification().getQueueUrl();
        AwsSqsProperties.StockNotificationQueue config = sqsProperties.getStockNotification();

        while (running.get()) {
            try {
                ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(config.getMaxMessages())
                        .waitTimeSeconds(config.getWaitTimeSeconds())
                        .visibilityTimeout(config.getVisibilityTimeoutSeconds())
                        .attributeNames(QueueAttributeName.ALL)
                        .messageAttributeNames("All")
                        .build();

                ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
                List<Message> messages = response.messages();

                if (messages.isEmpty()) {
                    log.debug("No messages received from SQS queue");
                    continue;
                }

                log.info("Received {} messages from SQS queue", messages.size());

                for (Message message : messages) {
                    try {
                        processMessage(message, queueUrl);
                    } catch (Exception e) {
                        log.error("Error processing SQS message {}: {}", message.messageId(), e.getMessage(), e);
                    }
                }

            } catch (SqsException e) {
                log.error("SQS error while polling messages: {}", e.getMessage(), e);
                sleepQuietly(config.getPollingIntervalMs());
            } catch (Exception e) {
                log.error("Unexpected error while polling messages: {}", e.getMessage(), e);
                sleepQuietly(config.getPollingIntervalMs());
            }
        }

        log.info("Stock notification SQS listener stopped");
    }

    private void processMessage(Message message, String queueUrl) {
        log.debug("Processing SQS message: {}", message.messageId());

        try {
            // Parse the message body
            Map<String, Object> payload = objectMapper.readValue(
                    message.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            // Extract fields from payload
            String userId = getStringValue(payload, "userId");
            String guestId = getStringValue(payload, "guestId");
            Long itemId = getLongValue(payload, "itemId");
            String sku = getStringValue(payload, "skuid");
            String screen = getStringValue(payload, "screen");
            String sourceType = getStringValue(payload, "sourceType");
            String sourceName = getStringValue(payload, "sourceName");

            // Validate required fields
            if ((userId == null || userId.isBlank()) && (guestId == null || guestId.isBlank())) {
                log.warn("Message {} has no userId or guestId, skipping", message.messageId());
                deleteMessage(message, queueUrl);
                return;
            }

            if (itemId == null || sku == null || sku.isBlank()) {
                log.warn("Message {} has missing itemId or sku, skipping", message.messageId());
                deleteMessage(message, queueUrl);
                return;
            }

            // Use userId as primary, fallback to guestId
            String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : guestId;

            // Create domain event
            StockNotificationEvent event = StockNotificationEvent.create(
                    message.messageId(),
                    message.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID),
                    effectiveUserId,
                    guestId,
                    itemId,
                    sku,
                    screen,
                    sourceType,
                    sourceName,
                    payload
            );

            // Save to database
            eventRepository.save(event);
            log.info("Saved stock notification event: id={}, userId={}, sku={}", event.getId(), effectiveUserId, sku);

            // Delete message from queue after successful processing
            deleteMessage(message, queueUrl);

        } catch (Exception e) {
            log.error("Failed to process message {}: {}", message.messageId(), e.getMessage(), e);
            // Don't delete the message - it will become visible again after visibility timeout
        }
    }

    private void deleteMessage(Message message, String queueUrl) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);
            log.debug("Deleted message {} from SQS queue", message.messageId());
        } catch (Exception e) {
            log.error("Failed to delete message {} from SQS queue: {}", message.messageId(), e.getMessage(), e);
        }
    }

    private String getStringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private Long getLongValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
