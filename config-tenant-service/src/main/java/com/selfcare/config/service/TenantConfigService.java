package com.selfcare.config.service;

import com.selfcare.config.domain.LayoutDocument;
import com.selfcare.config.domain.TenantConfig;
import com.selfcare.config.repository.LayoutRepository;
import com.selfcare.config.repository.TenantConfigRepository;
import com.selfcare.platform.common.web.NotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Short-TTL Redis caching (see application.yml {@code spring.cache.redis.time-to-live}) is
 * deliberate: config changes should propagate within seconds, not be invisible for the life of
 * the cache, since the entire "new feature = config, not release" pitch (Doc 1 sec 6.2) depends
 * on operators seeing their changes take effect quickly.
 */
@Service
public class TenantConfigService {

    private final TenantConfigRepository tenantConfigRepository;
    private final LayoutRepository layoutRepository;

    public TenantConfigService(TenantConfigRepository tenantConfigRepository, LayoutRepository layoutRepository) {
        this.tenantConfigRepository = tenantConfigRepository;
        this.layoutRepository = layoutRepository;
    }

    @Cacheable(cacheNames = "tenantConfig", key = "#tenantId")
    public TenantConfig getTenantConfig(String tenantId) {
        return tenantConfigRepository.findById(tenantId)
                .filter(TenantConfig::isActive)
                .orElseThrow(() -> new NotFoundException("No active tenant config for tenantId=" + tenantId));
    }

    @Cacheable(cacheNames = "tenantResolution", key = "'host:' + #host")
    public TenantConfig resolveByHost(String host) {
        return tenantConfigRepository.findByHostAliasesContainingAndActiveTrue(host)
                .orElseThrow(() -> new NotFoundException("No tenant resolves to host=" + host));
    }

    @Cacheable(cacheNames = "tenantResolution", key = "'flavor:' + #appFlavorId")
    public TenantConfig resolveByAppFlavor(String appFlavorId) {
        return tenantConfigRepository.findByAppFlavorIdsContainingAndActiveTrue(appFlavorId)
                .orElseThrow(() -> new NotFoundException("No tenant resolves to appFlavorId=" + appFlavorId));
    }

    @Cacheable(cacheNames = "tenantLayout", key = "#tenantId + ':' + #screenKey")
    public LayoutDocument getLayout(String tenantId, String screenKey) {
        return layoutRepository.findByTenantIdAndScreenKey(tenantId, screenKey)
                .orElseThrow(() -> new NotFoundException(
                        "No layout for tenantId=" + tenantId + ", screenKey=" + screenKey));
    }

    @CacheEvict(cacheNames = {"tenantConfig", "tenantResolution"}, key = "#config.tenantId")
    public TenantConfig upsertTenantConfig(TenantConfig config) {
        return tenantConfigRepository.save(config);
    }

    @CacheEvict(cacheNames = "tenantLayout", key = "#layout.tenantId + ':' + #layout.screenKey")
    public LayoutDocument upsertLayout(LayoutDocument layout) {
        layout.setVersion(layout.getVersion() + 1);
        return layoutRepository.save(layout);
    }
}
