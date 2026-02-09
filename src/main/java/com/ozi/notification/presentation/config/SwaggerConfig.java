package com.ozi.notification.presentation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for API documentation.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .description("Central notification service for handling push notifications and WhatsApp messages")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("OZI Team")
                                .email("dev@ozi.com")));
    }
}