package com.ozi.notification.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for CleverTap integration.
 */
@Component
@ConfigurationProperties(prefix = "notification.clevertap")
@Data
public class CleverTapProperties {

    /**
     * CleverTap Account ID
     */
    private String accountId;

    /**
     * CleverTap Passcode
     */
    private String passcode;

    /**
     * CleverTap Region (in1, us1, eu1, sg1, etc.)
     */
    private String region = "in1";

    /**
     * CleverTap Base URL (constructed from region)
     */
    private String baseUrl;
}