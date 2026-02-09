-- Migration V3: Create stock_notification_events table for SQS back-in-stock notifications
-- This table stores incoming SQS messages and tracks their processing status for CleverTap push notifications

CREATE TABLE IF NOT EXISTS stock_notification_events (
    id VARCHAR(36) PRIMARY KEY,

    -- SQS Message identification
    sqs_message_id VARCHAR(100) NOT NULL COMMENT 'SQS message ID for tracking',
    sqs_message_group_id VARCHAR(128) COMMENT 'FIFO queue message group ID',

    -- Payload fields from SQS message
    user_id VARCHAR(50) NOT NULL COMMENT 'User ID to notify (CleverTap Identity)',
    guest_id VARCHAR(50) COMMENT 'Guest ID (fallback if user_id not available)',
    item_id BIGINT NOT NULL COMMENT 'Product item ID',
    sku VARCHAR(50) NOT NULL COMMENT 'Product SKU',
    screen VARCHAR(50) COMMENT 'Screen for deep link (e.g., product)',
    source_type VARCHAR(50) COMMENT 'Source type (e.g., notification)',
    source_name VARCHAR(50) COMMENT 'Source name (e.g., back_in_stock)',

    -- Processing status
    status ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED') NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING=awaiting processing, PROCESSING=being sent, SENT=successfully sent to CleverTap, FAILED=send failed, SKIPPED=duplicate/filtered',

    -- Timestamps
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When the SQS message was received',
    processed_at TIMESTAMP NULL COMMENT 'When processing was attempted',
    sent_at TIMESTAMP NULL COMMENT 'When successfully sent to CleverTap',

    -- Error tracking
    error_message TEXT COMMENT 'Error details if processing failed',
    retry_count INT NOT NULL DEFAULT 0 COMMENT 'Number of processing attempts',

    -- Raw data for debugging
    raw_payload JSON COMMENT 'Original SQS message payload for debugging',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Generated column for date-based deduplication
    received_date DATE GENERATED ALWAYS AS (DATE(received_at)) STORED,

    -- Indexes for query performance
    INDEX idx_stock_notif_status (status),
    INDEX idx_stock_notif_user_id (user_id),
    INDEX idx_stock_notif_sku (sku),
    INDEX idx_stock_notif_received_at (received_at),
    INDEX idx_stock_notif_sqs_msg_id (sqs_message_id),

    -- Composite index for deduplication query (one notification per user per SKU per day)
    INDEX idx_stock_notif_dedup (user_id, sku, received_date)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT 'Stores back-in-stock SQS messages for daily 10 AM CleverTap push notifications';
