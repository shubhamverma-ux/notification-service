package com.ozi.notification.application.usecase;

import com.ozi.notification.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendNotificationUseCaseImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationServiceProvider notificationServiceProvider;

    private SendNotificationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendNotificationUseCaseImpl(notificationRepository, notificationServiceProvider);
    }

    @Test
    void shouldSendNotificationSuccessfully() throws NotificationException {
        // Given
        Notification savedNotification = Notification.create(
            NotificationType.PUSH,
            "user@example.com",
            "Test Title",
            "Test Message"
        );

        Notification sentNotification = savedNotification.markAsSent();

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification, sentNotification);
        when(notificationServiceProvider.sendNotification(any(Notification.class))).thenReturn(sentNotification);

        SendNotificationUseCase.SendNotificationRequest request = new SendNotificationUseCase.SendNotificationRequest() {
            @Override
            public NotificationType getType() {
                return NotificationType.PUSH;
            }

            @Override
            public String getRecipient() {
                return "user@example.com";
            }

            @Override
            public String getTitle() {
                return "Test Title";
            }

            @Override
            public String getMessage() {
                return "Test Message";
            }

            @Override
            public java.util.Map<String, String> getData() {
                return null;
            }

            @Override
            public String getDeepLink() {
                return null;
            }

            @Override
            public NotificationPriority getPriority() {
                return NotificationPriority.NORMAL;
            }
        };

        // When
        SendNotificationUseCase.SendNotificationResult result = useCase.execute(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getNotificationId()).isNotNull();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void shouldHandleNotificationSendingFailure() throws NotificationException {
        // Given
        Notification savedNotification = Notification.create(
            NotificationType.PUSH,
            "user@example.com",
            "Test Title",
            "Test Message"
        );

        NotificationException exception = new NotificationException(
            "Failed to send notification",
            savedNotification.getId(),
            NotificationType.PUSH
        );

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationServiceProvider.sendNotification(any(Notification.class))).thenThrow(exception);

        SendNotificationUseCase.SendNotificationRequest request = new SendNotificationUseCase.SendNotificationRequest() {
            @Override
            public NotificationType getType() {
                return NotificationType.PUSH;
            }

            @Override
            public String getRecipient() {
                return "user@example.com";
            }

            @Override
            public String getTitle() {
                return "Test Title";
            }

            @Override
            public String getMessage() {
                return "Test Message";
            }

            @Override
            public java.util.Map<String, String> getData() {
                return null;
            }

            @Override
            public String getDeepLink() {
                return null;
            }

            @Override
            public NotificationPriority getPriority() {
                return NotificationPriority.NORMAL;
            }
        };

        // When
        SendNotificationUseCase.SendNotificationResult result = useCase.execute(request);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Failed to send notification");
    }
}