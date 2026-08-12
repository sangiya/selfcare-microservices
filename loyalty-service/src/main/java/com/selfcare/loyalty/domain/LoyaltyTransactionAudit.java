package com.selfcare.loyalty.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Replaces the legacy {@code audit_log(...)} free-text calls with a real, queryable table.
 * Every service gets its own schema (Doc 1 sec 4.2) -- this table lives only in
 * loyalty-service's database, not the shared MySQL NDB cluster the legacy PHP app used.
 */
@Entity
@Table(
        name = "loyalty_transaction_audit",
        indexes = {
                @Index(name = "idx_audit_msisdn", columnList = "subscriberMsisdn"),
                @Index(name = "idx_audit_tenant_created", columnList = "tenantId, createdAt")
        })
public class LoyaltyTransactionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 32)
    private String subscriberMsisdn;

    @Column(length = 32)
    private String nationalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LoyaltyActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuditStatus status;

    @Column(length = 32)
    private String channel;

    @Column(length = 64)
    private String counterparty;

    private BigDecimal amount;

    @Column(length = 512)
    private String detail;

    @Column(nullable = false)
    private Instant createdAt;

    protected LoyaltyTransactionAudit() {
        // JPA
    }

    private LoyaltyTransactionAudit(Builder b) {
        this.tenantId = b.tenantId;
        this.subscriberMsisdn = b.subscriberMsisdn;
        this.nationalId = b.nationalId;
        this.actionType = b.actionType;
        this.status = b.status;
        this.channel = b.channel;
        this.counterparty = b.counterparty;
        this.amount = b.amount;
        this.detail = b.detail;
        this.createdAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getSubscriberMsisdn() {
        return subscriberMsisdn;
    }

    public String getNationalId() {
        return nationalId;
    }

    public LoyaltyActionType getActionType() {
        return actionType;
    }

    public AuditStatus getStatus() {
        return status;
    }

    public String getChannel() {
        return channel;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static final class Builder {
        private String tenantId;
        private String subscriberMsisdn;
        private String nationalId;
        private LoyaltyActionType actionType;
        private AuditStatus status;
        private String channel;
        private String counterparty;
        private BigDecimal amount;
        private String detail;

        public Builder tenantId(String v) {
            this.tenantId = v;
            return this;
        }

        public Builder subscriberMsisdn(String v) {
            this.subscriberMsisdn = v;
            return this;
        }

        public Builder nationalId(String v) {
            this.nationalId = v;
            return this;
        }

        public Builder actionType(LoyaltyActionType v) {
            this.actionType = v;
            return this;
        }

        public Builder status(AuditStatus v) {
            this.status = v;
            return this;
        }

        public Builder channel(String v) {
            this.channel = v;
            return this;
        }

        public Builder counterparty(String v) {
            this.counterparty = v;
            return this;
        }

        public Builder amount(BigDecimal v) {
            this.amount = v;
            return this;
        }

        public Builder detail(String v) {
            this.detail = v;
            return this;
        }

        public LoyaltyTransactionAudit build() {
            return new LoyaltyTransactionAudit(this);
        }
    }
}
