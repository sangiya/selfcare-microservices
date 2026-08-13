package com.selfcare.loyalty.service.impl;

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
import com.selfcare.loyalty.service.CustomerValidationClient;
import com.selfcare.loyalty.service.LoyaltyService;
import com.selfcare.loyalty.web.dto.ActivityItemResponse;
import com.selfcare.loyalty.web.dto.BalanceResponse;
import com.selfcare.loyalty.web.dto.HistoryEntryResponse;
import com.selfcare.loyalty.web.dto.RegisterRequest;
import com.selfcare.loyalty.web.dto.RegisterResponse;
import com.selfcare.platform.common.adapter.ApiAdapterRegistry;
import com.selfcare.platform.common.featureflag.FeatureFlagClient;
import com.selfcare.platform.common.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Java port of {@code scapp/StarPointsController.php} (Doc 1 sec 2.3 audit, Doc 5 sec 4 pilot
 * scope). Structural translation notes:
 * <ul>
 *   <li>Balance/registration/transfer/donation/history are all delegated to
 *       {@link LoyaltyCoreAdapter} -- this service owns orchestration and audit, not a local
 *       points ledger, matching the legacy controller's reliance on MIFE as source of truth.</li>
 *   <li>{@code audit_log(...)} calls become {@link LoyaltyTransactionAuditRepository} rows.</li>
 *   <li>{@code KafkaOtherServices}/{@code KafkaUtil::publishLog} calls become
 *       {@link LoyaltyEventPublisher} events on typed topics.</li>
 *   <li>FlySmiles/Amex partner transfers no longer send email directly from this service --
 *       they publish {@link PartnerRedemptionRequestedEvent} for notification-service to
 *       handle, and are gated behind a feature flag since not every operator has those
 *       partnerships (Doc 1 sec 6.2).</li>
 * </ul>
 */
@Service
public class LoyaltyServiceImpl implements LoyaltyService {

    private final ApiAdapterRegistry adapterRegistry;
    private final CustomerValidationClient customerValidationClient;
    private final LoyaltyTransactionAuditRepository auditRepository;
    private final LoyaltyEventPublisher eventPublisher;
    private final FeatureFlagClient featureFlagClient;

    public LoyaltyServiceImpl(
            ApiAdapterRegistry adapterRegistry,
            CustomerValidationClient customerValidationClient,
            LoyaltyTransactionAuditRepository auditRepository,
            LoyaltyEventPublisher eventPublisher,
            FeatureFlagClient featureFlagClient) {
        this.adapterRegistry = adapterRegistry;
        this.customerValidationClient = customerValidationClient;
        this.auditRepository = auditRepository;
        this.eventPublisher = eventPublisher;
        this.featureFlagClient = featureFlagClient;
    }

    @Override
    public BalanceResponse getBalance(String nationalId, String subscriberMsisdn) {
        validateOrThrow(nationalId, subscriberMsisdn, LoyaltyActionType.GET_BALANCE);
        AuditActor actor = new AuditActor(subscriberMsisdn, nationalId);
        try {
            LoyaltyCoreAdapter.PointsBalance balance = adapter().getBalance(subscriberMsisdn);
            audit(actor, LoyaltyActionType.GET_BALANCE, AuditStatus.SUCCESS, AuditMetadata.none());
            return new BalanceResponse(balance.currentBalance(), balance.redeemableBalance());
        } catch (LoyaltyCoreIntegrationException ex) {
            audit(actor, LoyaltyActionType.GET_BALANCE, AuditStatus.FAILURE, AuditMetadata.detail(ex.getMessage()));
            throw ex;
        }
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        validateOrThrow(request.nationalId(), request.msisdn(), LoyaltyActionType.REGISTER);
        AuditActor actor = new AuditActor(request.msisdn(), request.nationalId());
        try {
            LoyaltyCoreAdapter.RegisterCommand command = new LoyaltyCoreAdapter.RegisterCommand(
                    request.msisdn(), request.idType(), request.idNumber(), request.name(), request.address(), request.email());
            LoyaltyCoreAdapter.RegistrationResult result = adapter().register(command);
            audit(actor, LoyaltyActionType.REGISTER, AuditStatus.SUCCESS, AuditMetadata.detail("status=" + result.status()));
            return new RegisterResponse(result.status().name(), result.pinTransactionRef());
        } catch (LoyaltyCoreIntegrationException ex) {
            audit(actor, LoyaltyActionType.REGISTER, AuditStatus.FAILURE, AuditMetadata.detail(ex.getMessage()));
            throw ex;
        }
    }

    @Override
    @Transactional
    public void transfer(String nationalId, String fromMsisdn, TransferChannel channel, String toIdentifier, BigDecimal amount) {
        validateOrThrow(nationalId, fromMsisdn, LoyaltyActionType.TRANSFER);
        AuditActor actor = new AuditActor(fromMsisdn, nationalId);
        AuditMetadata transferMetadata = AuditMetadata.of(channel.name(), toIdentifier, amount, null);

        if (channel == TransferChannel.MOBILE) {
            try {
                adapter().transferPoints(new LoyaltyCoreAdapter.TransferCommand(fromMsisdn, toIdentifier, amount));
                audit(actor, LoyaltyActionType.TRANSFER, AuditStatus.SUCCESS, transferMetadata);
                eventPublisher.publishPointsTransfer(new PointsTransferEvent(
                        TenantContext.get(), "TRANSFER", fromMsisdn, toIdentifier, amount, Instant.now()));
            } catch (LoyaltyCoreIntegrationException ex) {
                audit(actor, LoyaltyActionType.TRANSFER, AuditStatus.FAILURE,
                        transferMetadata.withDetail(ex.getMessage()));
                throw ex;
            }
            return;
        }

        // FLYSMILES / AMEX: not every operator has these partnerships (Doc 1 sec 6.2 --
        // config/feature-flag driven, not hard-coded per operator).
        String flagName = "loyalty-partner-transfer-" + channel.name().toLowerCase();
        if (!featureFlagClient.isEnabled(flagName, false)) {
            audit(actor, LoyaltyActionType.PARTNER_REDEMPTION_REQUEST, AuditStatus.FAILURE,
                    transferMetadata.withDetail("channel not enabled for this operator"));
            throw new UnsupportedTransferChannelException(
                    "Transfer channel " + channel + " is not enabled for this operator");
        }

        eventPublisher.publishPartnerRedemptionRequested(
                new PartnerRedemptionRequestedEvent(TenantContext.get(), channel.name(), fromMsisdn, toIdentifier, amount, Instant.now()));
        audit(actor, LoyaltyActionType.PARTNER_REDEMPTION_REQUEST, AuditStatus.SUCCESS,
                transferMetadata.withDetail("partner redemption request queued"));
    }

    @Override
    public void donate(String nationalId, String msisdn, String donationAlias, BigDecimal amount) {
        validateOrThrow(nationalId, msisdn, LoyaltyActionType.DONATE);
        AuditActor actor = new AuditActor(msisdn, nationalId);
        AuditMetadata donationMetadata = AuditMetadata.of(null, donationAlias, amount, null);
        try {
            adapter().donatePoints(new LoyaltyCoreAdapter.DonateCommand(msisdn, donationAlias, amount));
            audit(actor, LoyaltyActionType.DONATE, AuditStatus.SUCCESS, donationMetadata);
            eventPublisher.publishPointsTransfer(
                    new PointsTransferEvent(TenantContext.get(), "DONATE", msisdn, donationAlias, amount, Instant.now()));
        } catch (LoyaltyCoreIntegrationException ex) {
            audit(actor, LoyaltyActionType.DONATE, AuditStatus.FAILURE, donationMetadata.withDetail(ex.getMessage()));
            throw ex;
        }
    }

    @Override
    public List<HistoryEntryResponse> getHistory(String nationalId, String subscriberMsisdn, int listSize) {
        validateOrThrow(nationalId, subscriberMsisdn, LoyaltyActionType.GET_ACTIVITY);
        LoyaltyCoreAdapter.PointsHistory history = adapter().getHistory(subscriberMsisdn, listSize);
        audit(new AuditActor(subscriberMsisdn, nationalId), LoyaltyActionType.GET_ACTIVITY, AuditStatus.SUCCESS, AuditMetadata.none());
        return history.entries().stream()
                .map(e -> new HistoryEntryResponse(e.transactionSerial(), e.transactionType(), e.merchant(), e.amount(), e.occurredAt()))
                .toList();
    }

    @Override
    public List<ActivityItemResponse> getRecentActivity(String subscriberMsisdn, int page, int size) {
        return auditRepository.findBySubscriberMsisdnOrderByCreatedAtDesc(subscriberMsisdn, PageRequest.of(page, size))
                .map(a -> new ActivityItemResponse(a.getId(), a.getActionType().name(), a.getStatus().name(), a.getChannel(),
                        a.getCounterparty(), a.getAmount(), a.getDetail(), a.getCreatedAt()))
                .toList();
    }

    private LoyaltyCoreAdapter adapter() {
        return adapterRegistry.resolve(LoyaltyCoreAdapter.class);
    }

    private void validateOrThrow(String nationalId, String subscriberMsisdn, LoyaltyActionType actionType) {
        if (!customerValidationClient.isValidCustomer(nationalId, subscriberMsisdn)) {
            audit(new AuditActor(subscriberMsisdn, nationalId), actionType, AuditStatus.FAILURE,
                    AuditMetadata.detail("customer validation failed"));
            throw new CustomerValidationException("Customer validation failed for the supplied nationalId/msisdn");
        }
    }

    private void audit(AuditActor actor, LoyaltyActionType actionType, AuditStatus status, AuditMetadata metadata) {
        LoyaltyTransactionAudit entry = LoyaltyTransactionAudit.builder()
                .tenantId(TenantContext.get())
                .subscriberMsisdn(actor.msisdn())
                .nationalId(actor.nationalId())
                .actionType(actionType)
                .status(status)
                .channel(metadata.channel())
                .counterparty(metadata.counterparty())
                .amount(metadata.amount())
                .detail(metadata.detail())
                .build();
        auditRepository.save(entry);
    }

    private record AuditActor(String msisdn, String nationalId) {
    }

    private record AuditMetadata(String channel, String counterparty, BigDecimal amount, String detail) {

        private static AuditMetadata none() {
            return new AuditMetadata(null, null, null, null);
        }

        private static AuditMetadata detail(String detail) {
            return new AuditMetadata(null, null, null, detail);
        }

        private static AuditMetadata of(String channel, String counterparty, BigDecimal amount, String detail) {
            return new AuditMetadata(channel, counterparty, amount, detail);
        }

        private AuditMetadata withDetail(String detail) {
            return new AuditMetadata(channel, counterparty, amount, detail);
        }
    }
}
