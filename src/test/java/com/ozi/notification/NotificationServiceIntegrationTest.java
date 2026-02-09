package com.ozi.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozi.notification.application.dto.SendNotificationRequestDto;
import com.ozi.notification.domain.NotificationType;
import com.ozi.notification.domain.NotificationPriority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class NotificationServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSendNotificationSuccessfully() throws Exception {
        // Given
        SendNotificationRequestDto request = SendNotificationRequestDto.builder()
                .type(NotificationType.PUSH)
                .recipient("test@example.com")
                .title("Test Notification")
                .message("This is a test notification")
                .priority(NotificationPriority.NORMAL)
                .build();

        // When & Then
        mockMvc.perform(post("/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.notificationId").exists());
    }

    @Test
    void shouldReturnBadRequestForInvalidRequest() throws Exception {
        // Given
        SendNotificationRequestDto request = SendNotificationRequestDto.builder()
                .type(NotificationType.PUSH)
                // Missing required fields
                .build();

        // When & Then
        mockMvc.perform(post("/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").exists());
    }

    @Test
    void shouldProcessPendingNotifications() throws Exception {
        // When & Then
        mockMvc.perform(post("/notifications/process-pending"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalProcessed").isNumber())
                .andExpect(jsonPath("$.totalSuccessful").isNumber())
                .andExpect(jsonPath("$.totalFailed").isNumber());
    }
}