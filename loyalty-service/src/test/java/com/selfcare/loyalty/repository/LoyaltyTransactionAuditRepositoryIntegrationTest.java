package com.selfcare.loyalty.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.selfcare.loyalty.domain.AuditStatus;
import com.selfcare.loyalty.domain.LoyaltyActionType;
import com.selfcare.loyalty.domain.LoyaltyTransactionAudit;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the JPA mapping + Flyway migration (V1__create_loyalty_transaction_audit.sql)
 * against a real MySQL instance rather than an in-memory substitute -- exactly the
 * "Testcontainers spins up real MySQL ... instead of mocking" automation requirement from
 * Doc 2 sec 3.6 / Doc 5 sec 6.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class LoyaltyTransactionAuditRepositoryIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("loyalty_service")
            .withUsername("loyalty_service")
            .withPassword("test-password-not-for-production");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private LoyaltyTransactionAuditRepository repository;

    @Test
    void savesAndQueriesAuditRowsByMsisdnMostRecentFirst() {
        String msisdn = "94771111111";

        repository.save(LoyaltyTransactionAudit.builder()
                .tenantId("acme-telecom")
                .subscriberMsisdn(msisdn)
                .nationalId("912345678V")
                .actionType(LoyaltyActionType.GET_BALANCE)
                .status(AuditStatus.SUCCESS)
                .build());

        repository.save(LoyaltyTransactionAudit.builder()
                .tenantId("acme-telecom")
                .subscriberMsisdn(msisdn)
                .nationalId("912345678V")
                .actionType(LoyaltyActionType.TRANSFER)
                .status(AuditStatus.SUCCESS)
                .channel("MOBILE")
                .counterparty("94779999999")
                .amount(new BigDecimal("25.00"))
                .build());

        Page<LoyaltyTransactionAudit> page =
                repository.findBySubscriberMsisdnOrderByCreatedAtDesc(msisdn, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getActionType()).isEqualTo(LoyaltyActionType.TRANSFER);
        assertThat(page.getContent().get(0).getAmount()).isEqualByComparingTo("25.00");
        assertThat(page.getContent().get(1).getActionType()).isEqualTo(LoyaltyActionType.GET_BALANCE);
    }
}
