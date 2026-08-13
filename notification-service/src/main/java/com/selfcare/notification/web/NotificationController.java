package com.selfcare.notification.web;

import com.selfcare.notification.domain.NotificationChannel;
import com.selfcare.notification.domain.NotificationRequest;
import com.selfcare.notification.repository.NotificationRequestRepository;
import com.selfcare.notification.service.NotificationDeliveryService;
import com.selfcare.platform.common.tenant.TenantContext;
import com.selfcare.platform.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direct REST path for other services/screens to request a notification synchronously. Delivery
 * goes through the pluggable provider adapter capability so operators can replace the default
 * implementation without changing this controller.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Validated
@Tag(name = "Notifications & Comms", description = "STARTER -- Doc 5 pilot domain 3 of 4; see README-TODO.md")
public class NotificationController {

    private final NotificationDeliveryService deliveryService;
    private final NotificationRequestRepository repository;

    public NotificationController(
            NotificationDeliveryService deliveryService,
            NotificationRequestRepository repository) {
        this.deliveryService = deliveryService;
        this.repository = repository;
    }

    @PostMapping
    public ApiResponse<NotificationRequest> send(
            @RequestParam @NotBlank String subscriberMsisdn,
            @RequestParam @NotNull NotificationChannel channel,
            @RequestParam @NotBlank String templateKey) {
        NotificationRequest request = new NotificationRequest();
        request.setTenantId(TenantContext.get());
        request.setSubscriberMsisdn(subscriberMsisdn);
        request.setChannel(channel);
        request.setTemplateKey(templateKey);
        request.setSourceEvent("direct-api");
        return ApiResponse.ok(deliveryService.deliver(request));
    }

    @GetMapping
    public ApiResponse<List<NotificationRequest>> listForSubscriber(@RequestParam @NotBlank String subscriberMsisdn) {
        return ApiResponse.ok(repository.findBySubscriberMsisdnOrderByCreatedAtDesc(subscriberMsisdn));
    }
}
