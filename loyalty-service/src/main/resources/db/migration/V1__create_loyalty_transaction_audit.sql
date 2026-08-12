-- Doc 1 sec 4.2: each newly migrated service gets its own schema rather than sharing tables
-- with other services (the implicit coupling the legacy MySQL NDB cluster created).
CREATE TABLE loyalty_transaction_audit (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id         VARCHAR(64)  NOT NULL,
    subscriber_msisdn VARCHAR(32)  NOT NULL,
    national_id       VARCHAR(32)  NULL,
    action_type       VARCHAR(32)  NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    channel           VARCHAR(32)  NULL,
    counterparty      VARCHAR(64)  NULL,
    amount            DECIMAL(18, 2) NULL,
    detail            VARCHAR(512) NULL,
    created_at        TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_audit_msisdn ON loyalty_transaction_audit (subscriber_msisdn);
CREATE INDEX idx_audit_tenant_created ON loyalty_transaction_audit (tenant_id, created_at);
