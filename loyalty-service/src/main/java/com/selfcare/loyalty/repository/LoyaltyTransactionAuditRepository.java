package com.selfcare.loyalty.repository;

import com.selfcare.loyalty.domain.LoyaltyTransactionAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyTransactionAuditRepository extends JpaRepository<LoyaltyTransactionAudit, Long> {

    Page<LoyaltyTransactionAudit> findBySubscriberMsisdnOrderByCreatedAtDesc(String subscriberMsisdn, Pageable pageable);
}
