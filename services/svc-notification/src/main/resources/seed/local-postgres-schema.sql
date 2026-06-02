CREATE TABLE IF NOT EXISTS notification_jobs (
    id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255) NOT NULL,
    channel VARCHAR(255) NOT NULL,
    template_id VARCHAR(255) NOT NULL,
    message VARCHAR(255) NOT NULL,
    delivery_attempts INTEGER NOT NULL,
    last_error VARCHAR(1000)
);
