package com.selfcare.reports.repository;

import com.selfcare.reports.domain.ReportRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRequestRepository extends JpaRepository<ReportRequest, Long> {

    List<ReportRequest> findBySubscriberMsisdnOrderByCreatedAtDesc(String subscriberMsisdn);
}
