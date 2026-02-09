-- Migration V2: Add tables for SQS-based stock notifications with scheduled delivery
-- This migration adds support for receiving stock events via SQS and scheduling notifications

-- ===================================================================
-- 1. stock_events: Store incoming SQS messages about stock changes
-- ===================================================================

CREATE TABLE IF NOT EXISTS stock_events (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(100) UNIQUE NOT NULL COMMENT 'SQS message ID for deduplication',
    event_type VARCHAR(50) NOT NULL COMMENT 'STOCK_BACK_IN_STOCK, STOCK_LOW, etc.',
    sku VARCHAR(50) NOT NULL COMMENT 'Product SKU that came back in stock',
    store_id VARCHAR(50) COMMENT 'Store/zone identifier',
    quantity_before INT COMMENT 'Stock quantity before the change',
    quantity_after INT COMMENT 'Stock quantity after the change',
    event_timestamp TIMESTAMP NOT NULL COMMENT 'When the stock change occurred',
    processed_at TIMESTAMP NULL COMMENT 'When we processed this event',
    status ENUM('RECEIVED', 'PROCESSED', 'FAILED') DEFAULT 'RECEIVED',
    error_message TEXT COMMENT 'Error details if processing failed',
    raw_message JSON COMMENT 'Full SQS message for debugging and reprocessing',

    INDEX idx_stock_events_sku (sku),
    INDEX idx_stock_events_status (status),
    INDEX idx_stock_events_timestamp (event_timestamp),
    INDEX idx_stock_events_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT 'Stores SQS messages about stock changes for processing';

-- ===================================================================
-- 2. user_notification_preferences: User preferences for notifications
-- ===================================================================

CREATE TABLE IF NOT EXISTS user_notification_preferences (
    user_id VARCHAR(50) PRIMARY KEY COMMENT 'User identifier (from user_notify table)',
    push_notifications_enabled BOOLEAN DEFAULT true COMMENT 'Whether user wants push notifications',
    whatsapp_enabled BOOLEAN DEFAULT true COMMENT 'Whether user wants WhatsApp notifications',
    preferred_timezone VARCHAR(50) DEFAULT 'Asia/Kolkata' COMMENT 'User timezone for scheduling',
    push_schedule_time TIME DEFAULT '10:00:00' COMMENT 'When to send push notifications (10 AM)',
    whatsapp_schedule_time TIME DEFAULT '16:00:00' COMMENT 'When to send WhatsApp notifications (4 PM)',
    language VARCHAR(10) DEFAULT 'en' COMMENT 'User preferred language',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_prefs_push_enabled (push_notifications_enabled),
    INDEX idx_user_prefs_whatsapp_enabled (whatsapp_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT 'User preferences for notification timing and channels';

-- ===================================================================
-- 3. scheduled_notifications: Queue for scheduled notifications
-- ===================================================================

CREATE TABLE IF NOT EXISTS scheduled_notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL COMMENT 'User to notify',
    sku VARCHAR(50) NOT NULL COMMENT 'SKU that came back in stock',
    notification_type ENUM('PUSH', 'WHATSAPP') NOT NULL COMMENT 'Type of notification to send',
    scheduled_time TIMESTAMP NOT NULL COMMENT 'When to send this notification',

    -- Notification content
    title TEXT COMMENT 'Notification title',
    message TEXT COMMENT 'Notification message',
    data JSON COMMENT 'Additional notification data',
    deep_link TEXT COMMENT 'Deep link URL',

    -- Processing metadata
    status ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    error_message TEXT,
    retry_count INT DEFAULT 0,

    INDEX idx_scheduled_user (user_id),
    INDEX idx_scheduled_sku (sku),
    INDEX idx_scheduled_time (scheduled_time),
    INDEX idx_scheduled_status (status),
    INDEX idx_scheduled_type (notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT 'Queue of notifications scheduled for specific times (10 AM Push, 4 PM WhatsApp)';

-- ===================================================================
-- 4. notification_history: Track all sent notifications for deduplication
-- ===================================================================

CREATE TABLE IF NOT EXISTS notification_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL COMMENT 'User who received the notification',
    sku VARCHAR(50) NOT NULL COMMENT 'SKU the notification was about',
    notification_type ENUM('PUSH', 'WHATSAPP') NOT NULL COMMENT 'Type of notification sent',
    scheduled_notification_id VARCHAR(36) COMMENT 'Reference to scheduled_notifications table',

    -- Content that was actually sent
    title TEXT COMMENT 'Title that was sent',
    message TEXT COMMENT 'Message that was sent',
    data JSON COMMENT 'Data payload that was sent',
    deep_link TEXT COMMENT 'Deep link that was sent',

    -- Delivery tracking
    sent_at TIMESTAMP NOT NULL COMMENT 'When the notification was sent',
    delivery_status ENUM('SENT', 'DELIVERED', 'FAILED') DEFAULT 'SENT',
    external_message_id VARCHAR(100) COMMENT 'CleverTap/WhatsApp message ID for tracking',
    error_message TEXT COMMENT 'Error details if delivery failed',

    INDEX idx_history_user (user_id),
    INDEX idx_history_sku (sku),
    INDEX idx_history_type (notification_type),
    INDEX idx_history_sent_at (sent_at),
    INDEX idx_history_external_id (external_message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT 'History of all notifications sent to users for deduplication and analytics';

-- ===================================================================
-- 5. Add indexes to existing user_notify table for performance
-- ===================================================================

-- Note: These ALTER statements are commented out as user_notify table may not exist
-- Uncomment and run manually if needed

-- ALTER TABLE user_notify ADD INDEX idx_user_notify_sku (sku);
-- ALTER TABLE user_notify ADD INDEX idx_user_notify_user_id (user_id);
-- ALTER TABLE user_notify ADD INDEX idx_user_notify_user_sku (user_id, sku);

-- ===================================================================
-- 6. Insert default preferences for existing users (optional)
-- ===================================================================

-- Note: Commented out as user_notify table may not exist
-- INSERT IGNORE INTO user_notification_preferences (user_id)
-- SELECT DISTINCT CAST(user_id AS CHAR(50)) as user_id
-- FROM user_notify
-- WHERE user_id IS NOT NULL;

-- ===================================================================
-- Comments and Documentation
-- ===================================================================

-- This migration adds support for:
-- 1. Receiving stock events via SQS from the inventory service
-- 2. User preferences for notification timing and channels
-- 3. Scheduled notifications queue (Push at 10 AM, WhatsApp at 4 PM)
-- 4. Notification history for deduplication and analytics
-- 5. Performance indexes on the existing user_notify table

-- The flow is:
-- 1. Inventory service sends SQS message when stock comes back
-- 2. stock_events table stores the message
-- 3. System finds users in user_notify table who want this SKU
-- 4. Checks user_notification_preferences for timing preferences
-- 5. Creates entries in scheduled_notifications for 10 AM and 4 PM
-- 6. Cron jobs process scheduled_notifications at the right times
-- 7. Records sent notifications in notification_history