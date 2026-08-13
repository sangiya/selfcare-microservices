package com.selfcare.notification.adapter;

import com.selfcare.notification.domain.NotificationRequest;
import com.selfcare.notification.domain.NotificationStatus;
import com.selfcare.platform.common.adapter.ApiAdapter;

/**
 * Stable provider capability for notification delivery. Operators can override the default
 * implementation with tenant-specific adapters without changing controller/listener logic.
 */
public interface NotificationProviderAdapter extends ApiAdapter {

    DeliveryResult send(NotificationRequest request);

    record DeliveryResult(NotificationStatus status) {
    }
}
