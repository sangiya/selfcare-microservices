package com.selfcare.notification.repository;

import com.selfcare.notification.domain.NotificationRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {

    List<NotificationRequest> findBySubscriberMsisdnOrderByCreatedAtDesc(String subscriberMsisdn);
}
