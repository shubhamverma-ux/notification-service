package com.ozi.notification.infrastructure.service;

import com.ozi.notification.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of NotificationServiceProvider.
 * Routes notifications to appropriate services based on notification type.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceProviderImpl implements NotificationServiceProvider {

    private final List<NotificationService> notificationServices;

    @Override
    public Notification sendNotification(Notification notification) throws NotificationException {
        log.debug("Routing notification {} of type {} to appropriate service",
                 notification.getId(), notification.getType());

        // Find the service that can handle this notification type
        NotificationService service = notificationServices.stream()
                .filter(s -> s.canHandle(notification.getType()))
                .findFirst()
                .orElseThrow(() -> new NotificationException(
                    "No notification service found for type: " + notification.getType(),
                    notification.getId(),
                    notification.getType()
                ));

        log.debug("Found service {} for notification type {}",
                 service.getClass().getSimpleName(), notification.getType());

        // Delegate to the appropriate service
        return service.sendNotification(notification);
    }
}