CREATE TABLE notification_request (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id         VARCHAR(64)  NOT NULL,
    subscriber_msisdn VARCHAR(32)  NOT NULL,
    channel           VARCHAR(16)  NOT NULL,
    template_key      VARCHAR(64)  NOT NULL,
    payload_json      VARCHAR(1024) NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'QUEUED',
    source_event      VARCHAR(128) NULL,
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_notification_request_msisdn ON notification_request (subscriber_msisdn);
