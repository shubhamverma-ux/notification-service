package com.ozi.notification.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for WhatsApp integration.
 */
@Component
@ConfigurationProperties(prefix = "notification.whatsapp")
@Data
public class WhatsAppProperties {

    /**
     * WhatsApp Business API URL
     */
    private String apiUrl;

    /**
     * WhatsApp API Key/Token
     */
    private String apiKey;

    /**
     * WhatsApp Business Account ID (optional)
     */
    private String accountId;

    /**
     * WhatsApp Phone Number ID (optional)
     */
    private String phoneNumberId;
}