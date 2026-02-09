package com.ozi.notification.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Flyway configuration to auto-repair failed migrations before migrating.
 */
@Configuration
@Slf4j
public class FlywayRepairCallback {

    @Bean
    @Profile("dev")
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Running Flyway repair before migration...");
            flyway.repair();
            log.info("Running Flyway migrate...");
            flyway.migrate();
        };
    }
}
