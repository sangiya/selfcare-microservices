package com.selfcare.loyalty.service;

import com.selfcare.loyalty.domain.TransferChannel;
import com.selfcare.loyalty.web.dto.ActivityItemResponse;
import com.selfcare.loyalty.web.dto.BalanceResponse;
import com.selfcare.loyalty.web.dto.HistoryEntryResponse;
import com.selfcare.loyalty.web.dto.RegisterRequest;
import com.selfcare.loyalty.web.dto.RegisterResponse;
import java.math.BigDecimal;
import java.util.List;

public interface LoyaltyService {

    BalanceResponse getBalance(String nationalId, String subscriberMsisdn);

    RegisterResponse register(RegisterRequest request);

    void transfer(String nationalId, String fromMsisdn, TransferChannel channel, String toIdentifier, BigDecimal amount);

    void donate(String nationalId, String msisdn, String donationAlias, BigDecimal amount);

    List<HistoryEntryResponse> getHistory(String nationalId, String subscriberMsisdn, int listSize);

    List<ActivityItemResponse> getRecentActivity(String subscriberMsisdn, int page, int size);
}
