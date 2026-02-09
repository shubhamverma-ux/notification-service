package com.ozi.notification.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for AWS SQS integration.
 */
@Component
@ConfigurationProperties(prefix = "aws.sqs")
@Data
public class AwsSqsProperties {

    /**
     * AWS region for SQS
     */
    private String region = "ap-south-1";

    /**
     * AWS access key ID
     */
    private String accessKeyId;

    /**
     * AWS secret access key
     */
    private String secretAccessKey;

    /**
     * Stock notification queue configuration
     */
    private StockNotificationQueue stockNotification = new StockNotificationQueue();

    @Data
    public static class StockNotificationQueue {
        /**
         * SQS queue URL for stock notifications
         */
        private String queueUrl;

        /**
         * Maximum number of messages to receive in one poll
         */
        private int maxMessages = 10;

        /**
         * Wait time in seconds for long polling
         */
        private int waitTimeSeconds = 20;

        /**
         * Visibility timeout in seconds
         */
        private int visibilityTimeoutSeconds = 30;

        /**
         * Whether the SQS listener is enabled
         */
        private boolean enabled = true;

        /**
         * Polling interval in milliseconds when no messages are received
         */
        private long pollingIntervalMs = 5000;
    }
}
