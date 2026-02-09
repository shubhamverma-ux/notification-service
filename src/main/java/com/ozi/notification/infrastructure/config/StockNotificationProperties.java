package com.ozi.notification.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for stock notifications.
 */
@Component
@ConfigurationProperties(prefix = "notification.stock")
@Data
public class StockNotificationProperties {

    /**
     * CleverTap campaign ID for back-in-stock notifications
     */
    private String clevertapCampaignId;

    /**
     * Default notification title
     */
    private String defaultTitle = "Back in Stock!";

    /**
     * Default notification message template.
     * Supports placeholders: {sku}, {itemId}
     */
    private String defaultMessageTemplate = "Product {sku} is now available";

    /**
     * Whether to include item data in CleverTap kvs
     */
    private boolean includeItemData = true;
}
