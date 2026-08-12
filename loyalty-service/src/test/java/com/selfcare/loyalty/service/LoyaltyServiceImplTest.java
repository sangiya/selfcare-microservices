package com.selfcare.loyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.selfcare.loyalty.adapter.LoyaltyCoreAdapter;
import com.selfcare.loyalty.domain.AuditStatus;
import com.selfcare.loyalty.domain.LoyaltyActionType;
import com.selfcare.loyalty.domain.LoyaltyTransactionAudit;
import com.selfcare.loyalty.domain.TransferChannel;
import com.selfcare.loyalty.event.LoyaltyEventPublisher;
import com.selfcare.loyalty.exception.CustomerValidationException;
import com.selfcare.loyalty.exception.UnsupportedTransferChannelException;
import com.selfcare.loyalty.repository.LoyaltyTransactionAuditRepository;
import com.selfcare.loyalty.service.impl.LoyaltyServiceImpl;
import com.selfcare.loyalty.web.dto.BalanceResponse;
import com.selfcare.platform.common.adapter.ApiAdapterRegistry;
import com.selfcare.platform.common.featureflag.FeatureFlagClient;
import com.selfcare.platform.common.tenant.TenantContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceImplTest {

    private static final String TENANT = "acme-telecom";
    private static final String NIC = "912345678V";
    private static final String MSISDN = "94771234567";

    @Mock
    private ApiAdapterRegistry adapterRegistry;
    @Mock
    private LoyaltyCoreAdapter loyaltyCoreAdapter;
    @Mock
    private CustomerValidationClient customerValidationClient;
    @Mock
    private LoyaltyTransactionAuditRepository auditRepository;
    @Mock
    private LoyaltyEventPublisher eventPublisher;
    @Mock
    private FeatureFlagClient featureFlagClient;

    private LoyaltyServiceImpl loyaltyService;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);
        loyaltyService = new LoyaltyServiceImpl(
                adapterRegistry, customerValidationClient, auditRepository, eventPublisher, featureFlagClient);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getBalance_returnsBalanceWhenCustomerIsValid() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        when(adapterRegistry.resolve(LoyaltyCoreAdapter.class)).thenReturn(loyaltyCoreAdapter);
        when(loyaltyCoreAdapter.getBalance(MSISDN))
                .thenReturn(new LoyaltyCoreAdapter.PointsBalance(new BigDecimal("1200.50"), new BigDecimal("900.00")));

        BalanceResponse response = loyaltyService.getBalance(NIC, MSISDN);

        assertThat(response.currentBalance()).isEqualByComparingTo("1200.50");
        assertThat(response.redeemableBalance()).isEqualByComparingTo("900.00");
        verify(auditRepository).save(argThatAudit(LoyaltyActionType.GET_BALANCE, AuditStatus.SUCCESS));
    }

    @Test
    void getBalance_throwsAndAuditsWhenCustomerInvalid() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(false);

        assertThatThrownBy(() -> loyaltyService.getBalance(NIC, MSISDN))
                .isInstanceOf(CustomerValidationException.class);

        verify(auditRepository).save(argThatAudit(LoyaltyActionType.GET_BALANCE, AuditStatus.FAILURE));
    }

    @Test
    void transfer_mobileChannel_callsAdapterAndPublishesEvent() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        when(adapterRegistry.resolve(LoyaltyCoreAdapter.class)).thenReturn(loyaltyCoreAdapter);

        loyaltyService.transfer(NIC, MSISDN, TransferChannel.MOBILE, "94779876543", new BigDecimal("50.00"));

        verify(loyaltyCoreAdapter).transferPoints(any());
        verify(eventPublisher).publishPointsTransfer(any());
        verify(auditRepository).save(argThatAudit(LoyaltyActionType.TRANSFER, AuditStatus.SUCCESS));
    }

    @Test
    void transfer_flySmilesChannel_deniedWhenFlagDisabled() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        when(featureFlagClient.isEnabled("loyalty-partner-transfer-flysmiles", false)).thenReturn(false);

        assertThatThrownBy(() ->
                        loyaltyService.transfer(NIC, MSISDN, TransferChannel.FLYSMILES, "FS-MEMBER-1", new BigDecimal("50.00")))
                .isInstanceOf(UnsupportedTransferChannelException.class);

        verify(eventPublisher, org.mockito.Mockito.never()).publishPartnerRedemptionRequested(any());
    }

    @Test
    void transfer_flySmilesChannel_publishesEventWhenFlagEnabled() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        when(featureFlagClient.isEnabled("loyalty-partner-transfer-flysmiles", false)).thenReturn(true);

        loyaltyService.transfer(NIC, MSISDN, TransferChannel.FLYSMILES, "FS-MEMBER-1", new BigDecimal("50.00"));

        verify(eventPublisher).publishPartnerRedemptionRequested(any());
        verify(loyaltyCoreAdapter, org.mockito.Mockito.never()).transferPoints(any());
    }

    private static LoyaltyTransactionAudit argThatAudit(LoyaltyActionType actionType, AuditStatus status) {
        return org.mockito.ArgumentMatchers.argThat(
                audit -> audit.getActionType() == actionType && audit.getStatus() == status);
    }
}
