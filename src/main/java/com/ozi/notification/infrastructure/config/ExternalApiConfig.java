package com.ozi.notification.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for external API integrations.
 */
@Configuration
@RequiredArgsConstructor
public class ExternalApiConfig {

    private final CleverTapProperties cleverTapProperties;

    /**
     * Initialize CleverTap base URL based on region.
     */
    @PostConstruct
    public void initializeCleverTapBaseUrl() {
        if (cleverTapProperties.getBaseUrl() == null || cleverTapProperties.getBaseUrl().isEmpty()) {
            String baseUrl = String.format("https://%s.api.clevertap.com",
                    cleverTapProperties.getRegion());
            cleverTapProperties.setBaseUrl(baseUrl);
        }
    }
}