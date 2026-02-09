package com.ozi.notification.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AWS SQS configuration.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("!'${aws.sqs.access-key-id:}'.isEmpty() && !'${aws.sqs.secret-access-key:}'.isEmpty()")
public class AwsSqsConfig {

    private final AwsSqsProperties sqsProperties;

    @Bean
    public SqsClient sqsClient() {
        log.info("Creating SQS client for region: {}", sqsProperties.getRegion());

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                sqsProperties.getAccessKeyId(),
                sqsProperties.getSecretAccessKey()
        );

        return SqsClient.builder()
                .region(Region.of(sqsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
