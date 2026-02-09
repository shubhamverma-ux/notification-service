-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    title TEXT,
    message TEXT,
    data JSON,
    deep_link TEXT,
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NULL,
    error_message TEXT,
    metadata JSON,

    INDEX idx_notifications_status (status),
    INDEX idx_notifications_recipient (recipient),
    INDEX idx_notifications_created_at (created_at),
    INDEX idx_notifications_type (type)
);

-- Add comments for documentation
ALTER TABLE notifications COMMENT = 'Stores notification requests and their delivery status';