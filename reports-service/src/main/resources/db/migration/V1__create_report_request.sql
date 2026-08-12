CREATE TABLE report_request (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id         VARCHAR(64)  NOT NULL,
    subscriber_msisdn VARCHAR(32)  NOT NULL,
    report_type       VARCHAR(32)  NOT NULL,
    from_date         DATE         NULL,
    to_date           DATE         NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    result_location   VARCHAR(512) NULL,
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_report_request_msisdn ON report_request (subscriber_msisdn);
