package com.selfcare.notification.service;

import com.selfcare.notification.adapter.NotificationProviderAdapter;
import com.selfcare.notification.domain.NotificationRequest;
import com.selfcare.notification.domain.NotificationStatus;
import com.selfcare.notification.repository.NotificationRequestRepository;
import com.selfcare.platform.common.adapter.ApiAdapterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

    private final ApiAdapterRegistry adapterRegistry;
    private final NotificationRequestRepository repository;

    public NotificationDeliveryService(ApiAdapterRegistry adapterRegistry, NotificationRequestRepository repository) {
        this.adapterRegistry = adapterRegistry;
        this.repository = repository;
    }

    public NotificationRequest deliver(NotificationRequest request) {
        try {
            NotificationProviderAdapter adapter = adapterRegistry.resolve(NotificationProviderAdapter.class);
            NotificationProviderAdapter.DeliveryResult result = adapter.send(request);
            request.setStatus(result.status());
        } catch (RuntimeException ex) {
            log.warn(
                    "Notification delivery failed: tenantId={}, channel={}, subscriberMsisdn={}, templateKey={}, sourceEvent={}",
                    request.getTenantId(),
                    request.getChannel(),
                    request.getSubscriberMsisdn(),
                    request.getTemplateKey(),
                    request.getSourceEvent(),
                    ex);
            request.setStatus(NotificationStatus.FAILED);
        }
        return repository.save(request);
    }
}
