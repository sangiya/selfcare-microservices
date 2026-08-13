package com.selfcare.notification.adapter;

import com.selfcare.notification.domain.NotificationRequest;
import com.selfcare.notification.domain.NotificationStatus;
import com.selfcare.platform.common.adapter.ApiAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default cross-tenant adapter used until an operator-specific SMS/push/email provider is wired.
 * It gives the service a real delivery path and can be overridden per tenant later.
 */
@Component
public class LoggingNotificationProviderAdapter implements NotificationProviderAdapter {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationProviderAdapter.class);

    @Override
    public DeliveryResult send(NotificationRequest request) {
        log.info(
                "Dispatching notification via default provider: tenantId={}, channel={}, subscriberMsisdn={}, templateKey={}, sourceEvent={}",
                request.getTenantId(),
                request.getChannel(),
                request.getSubscriberMsisdn(),
                request.getTemplateKey(),
                request.getSourceEvent());
        return new DeliveryResult(NotificationStatus.SENT);
    }

    @Override
    public String tenantId() {
        return ApiAdapter.ALL_TENANTS;
    }
}
