package com.selfcare.loyalty.adapter;

import com.selfcare.platform.common.adapter.ApiAdapter;
import java.math.BigDecimal;
import java.util.List;

/**
 * The stable interface every operator's loyalty core system is adapted to (Doc 1 sec 6.3).
 * The legacy PHP controller called this "the mife_post_request calls to starPointsAPI"; here
 * it is a first-class, independently-testable interface with one concrete implementation per
 * integration style. Onboarding an operator whose loyalty core ISN'T MIFE Star Points means
 * writing one new class that implements this interface -- {@code LoyaltyService} never changes.
 */
public interface LoyaltyCoreAdapter extends ApiAdapter {

    PointsBalance getBalance(String subscriberMsisdn);

    RegistrationResult register(RegisterCommand command);

    void transferPoints(TransferCommand command);

    void donatePoints(DonateCommand command);

    PointsHistory getHistory(String subscriberMsisdn, int listSize);

    record PointsBalance(BigDecimal currentBalance, BigDecimal redeemableBalance) {
    }

    record RegisterCommand(
            String subscriberMsisdn, String idType, String idNumber, String name, String address, String email) {
    }

    enum RegistrationStatus {
        REGISTERED,
        PIN_SENT
    }

    record RegistrationResult(RegistrationStatus status, String pinTransactionRef) {
    }

    record TransferCommand(String fromSubscriberMsisdn, String toSubscriberMsisdn, BigDecimal amount) {
    }

    record DonateCommand(String subscriberMsisdn, String donationAlias, BigDecimal amount) {
    }

    record PointsHistoryEntry(
            String transactionSerial, String transactionType, String merchant, BigDecimal amount, String occurredAt) {
    }

    record PointsHistory(List<PointsHistoryEntry> entries) {
    }
}
