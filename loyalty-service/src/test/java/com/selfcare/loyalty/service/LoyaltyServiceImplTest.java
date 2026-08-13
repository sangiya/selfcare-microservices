package com.selfcare.loyalty.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.selfcare.loyalty.adapter.LoyaltyCoreAdapter;
import com.selfcare.loyalty.domain.AuditStatus;
import com.selfcare.loyalty.domain.LoyaltyActionType;
import com.selfcare.loyalty.domain.LoyaltyTransactionAudit;
import com.selfcare.loyalty.domain.TransferChannel;
import com.selfcare.loyalty.event.LoyaltyEventPublisher;
import com.selfcare.loyalty.event.PartnerRedemptionRequestedEvent;
import com.selfcare.loyalty.event.PointsTransferEvent;
import com.selfcare.loyalty.exception.CustomerValidationException;
import com.selfcare.loyalty.exception.LoyaltyCoreIntegrationException;
import com.selfcare.loyalty.exception.UnsupportedTransferChannelException;
import com.selfcare.loyalty.repository.LoyaltyTransactionAuditRepository;
import com.selfcare.loyalty.service.impl.LoyaltyServiceImpl;
import com.selfcare.loyalty.web.dto.BalanceResponse;
import com.selfcare.loyalty.web.dto.RegisterRequest;
import com.selfcare.platform.common.adapter.ApiAdapterRegistry;
import com.selfcare.platform.common.adapter.ApiAdapter;
import com.selfcare.platform.common.featureflag.FeatureFlagClient;
import com.selfcare.platform.common.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private LoyaltyCoreAdapter loyaltyCoreAdapter;
    @Mock
    private CustomerValidationClient customerValidationClient;
    @Mock
    private LoyaltyTransactionAuditRepository auditRepository;
    @Mock
    private FeatureFlagClient featureFlagClient;

    private LoyaltyServiceImpl loyaltyService;
    private ApiAdapterRegistry adapterRegistry;
    private RecordingLoyaltyEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);
        adapterRegistry = new ApiAdapterRegistry();
        when(loyaltyCoreAdapter.tenantId()).thenReturn(ApiAdapter.ALL_TENANTS);
        adapterRegistry.register(LoyaltyCoreAdapter.class, List.of(loyaltyCoreAdapter));
        eventPublisher = new RecordingLoyaltyEventPublisher();
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
    void getBalance_rethrowsAdapterFailureAndAuditsFailure() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        when(loyaltyCoreAdapter.getBalance(MSISDN))
                .thenThrow(new LoyaltyCoreIntegrationException("balance lookup failed"));

        assertThatThrownBy(() -> loyaltyService.getBalance(NIC, MSISDN))
                .isInstanceOf(LoyaltyCoreIntegrationException.class);

        verify(auditRepository).save(argThatAudit(LoyaltyActionType.GET_BALANCE, AuditStatus.FAILURE));
    }

    @Test
    void transfer_mobileChannel_callsAdapterAndPublishesEvent() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);

        loyaltyService.transfer(NIC, MSISDN, TransferChannel.MOBILE, "94779876543", new BigDecimal("50.00"));

        verify(loyaltyCoreAdapter).transferPoints(any());
        assertThat(eventPublisher.lastPointsTransferEvent).isNotNull();
        assertThat(eventPublisher.lastPointsTransferEvent.eventType()).isEqualTo("TRANSFER");
        verify(auditRepository).save(argThatAudit(LoyaltyActionType.TRANSFER, AuditStatus.SUCCESS));
    }

    @Test
    void transfer_flySmilesChannel_deniedWhenFlagDisabled() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        when(featureFlagClient.isEnabled("loyalty-partner-transfer-flysmiles", false)).thenReturn(false);

        assertThatThrownBy(() ->
                        loyaltyService.transfer(NIC, MSISDN, TransferChannel.FLYSMILES, "FS-MEMBER-1", new BigDecimal("50.00")))
                .isInstanceOf(UnsupportedTransferChannelException.class);

        assertThat(eventPublisher.lastPartnerRedemptionRequestedEvent).isNull();
    }

    @Test
    void transfer_flySmilesChannel_publishesEventWhenFlagEnabled() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        when(featureFlagClient.isEnabled("loyalty-partner-transfer-flysmiles", false)).thenReturn(true);

        loyaltyService.transfer(NIC, MSISDN, TransferChannel.FLYSMILES, "FS-MEMBER-1", new BigDecimal("50.00"));

        assertThat(eventPublisher.lastPartnerRedemptionRequestedEvent).isNotNull();
        assertThat(eventPublisher.lastPartnerRedemptionRequestedEvent.channel()).isEqualTo("FLYSMILES");
        verify(loyaltyCoreAdapter, never()).transferPoints(any());
    }

    @Test
    void register_returnsAdapterResponseAndAuditsSuccess() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        when(loyaltyCoreAdapter.register(any()))
                .thenReturn(new LoyaltyCoreAdapter.RegistrationResult(
                        LoyaltyCoreAdapter.RegistrationStatus.PIN_SENT, "TX-99"));

        var response = loyaltyService.register(
                new RegisterRequest(NIC, MSISDN, "NIC", NIC, "Test User", "Colombo", "test@example.com"));

        assertThat(response.status()).isEqualTo("PIN_SENT");
        assertThat(response.pinTransactionRef()).isEqualTo("TX-99");
        verify(auditRepository).save(argThatAudit(LoyaltyActionType.REGISTER, AuditStatus.SUCCESS));
    }

    @Test
    void register_rethrowsAdapterFailureAndAuditsFailure() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        when(loyaltyCoreAdapter.register(any()))
                .thenThrow(new LoyaltyCoreIntegrationException("registration failed"));

        assertThatThrownBy(() -> loyaltyService.register(
                        new RegisterRequest(NIC, MSISDN, "NIC", NIC, "Test User", "Colombo", "test@example.com")))
                .isInstanceOf(LoyaltyCoreIntegrationException.class);

        verify(auditRepository).save(argThatAudit(LoyaltyActionType.REGISTER, AuditStatus.FAILURE));
    }

    @Test
    void transfer_mobileChannel_auditsFailureWhenAdapterThrows() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        doThrow(new LoyaltyCoreIntegrationException("transfer failed")).when(loyaltyCoreAdapter).transferPoints(any());

        assertThatThrownBy(() ->
                        loyaltyService.transfer(NIC, MSISDN, TransferChannel.MOBILE, "94779876543", new BigDecimal("50.00")))
                .isInstanceOf(LoyaltyCoreIntegrationException.class);

        verify(auditRepository).save(argThatAudit(LoyaltyActionType.TRANSFER, AuditStatus.FAILURE));
    }

    @Test
    void donate_publishesEventAndAuditsSuccess() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);

        loyaltyService.donate(NIC, MSISDN, "charity", new BigDecimal("25.00"));

        assertThat(eventPublisher.lastPointsTransferEvent).isNotNull();
        assertThat(eventPublisher.lastPointsTransferEvent.eventType()).isEqualTo("DONATE");
        verify(auditRepository).save(argThatAudit(LoyaltyActionType.DONATE, AuditStatus.SUCCESS));
    }

    @Test
    void donate_rethrowsAdapterFailureAndAuditsFailure() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        doThrow(new LoyaltyCoreIntegrationException("donation failed")).when(loyaltyCoreAdapter).donatePoints(any());

        assertThatThrownBy(() -> loyaltyService.donate(NIC, MSISDN, "charity", new BigDecimal("25.00")))
                .isInstanceOf(LoyaltyCoreIntegrationException.class);

        verify(auditRepository).save(argThatAudit(LoyaltyActionType.DONATE, AuditStatus.FAILURE));
    }

    @Test
    void getHistory_mapsAdapterEntriesAndAuditsSuccess() {
        when(customerValidationClient.isValidCustomer(NIC, MSISDN)).thenReturn(true);
        when(loyaltyCoreAdapter.getHistory(MSISDN, 5)).thenReturn(new LoyaltyCoreAdapter.PointsHistory(List.of(
                new LoyaltyCoreAdapter.PointsHistoryEntry("TX-1", "EARN", "Store", new BigDecimal("15.00"), "2026-08-10"))));

        var history = loyaltyService.getHistory(NIC, MSISDN, 5);

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().transactionSerial()).isEqualTo("TX-1");
        assertThat(history.getFirst().amount()).isEqualByComparingTo("15.00");
        verify(auditRepository).save(argThatAudit(LoyaltyActionType.GET_ACTIVITY, AuditStatus.SUCCESS));
    }

    @Test
    void getRecentActivity_mapsAuditRowsToApiResponse() {
        LoyaltyTransactionAudit audit = LoyaltyTransactionAudit.builder()
                .tenantId(TENANT)
                .subscriberMsisdn(MSISDN)
                .nationalId(NIC)
                .actionType(LoyaltyActionType.DONATE)
                .status(AuditStatus.SUCCESS)
                .channel("SMS")
                .counterparty("charity")
                .amount(new BigDecimal("25.00"))
                .detail("ok")
                .build();
        when(auditRepository.findBySubscriberMsisdnOrderByCreatedAtDesc(MSISDN, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(new ArrayList<>(List.of(audit))));

        var activity = loyaltyService.getRecentActivity(MSISDN, 0, 5);

        assertThat(activity).hasSize(1);
        assertThat(activity.getFirst().actionType()).isEqualTo("DONATE");
        assertThat(activity.getFirst().status()).isEqualTo("SUCCESS");
        assertThat(activity.getFirst().counterparty()).isEqualTo("charity");
        assertThat(activity.getFirst().amount()).isEqualByComparingTo("25.00");
    }

    private static LoyaltyTransactionAudit argThatAudit(LoyaltyActionType actionType, AuditStatus status) {
        return org.mockito.ArgumentMatchers.argThat(
                audit -> audit.getActionType() == actionType && audit.getStatus() == status);
    }

    private static final class RecordingLoyaltyEventPublisher extends LoyaltyEventPublisher {

        private PointsTransferEvent lastPointsTransferEvent;
        private PartnerRedemptionRequestedEvent lastPartnerRedemptionRequestedEvent;

        private RecordingLoyaltyEventPublisher() {
            super(null);
        }

        @Override
        public void publishPointsTransfer(PointsTransferEvent event) {
            lastPointsTransferEvent = event;
        }

        @Override
        public void publishPartnerRedemptionRequested(PartnerRedemptionRequestedEvent event) {
            lastPartnerRedemptionRequestedEvent = event;
        }
    }
}
