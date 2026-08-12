package com.selfcare.config.repository;

import com.selfcare.config.domain.TenantConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TenantConfigRepository extends MongoRepository<TenantConfig, String> {

    Optional<TenantConfig> findByHostAliasesContainingAndActiveTrue(String host);

    Optional<TenantConfig> findByAppFlavorIdsContainingAndActiveTrue(String appFlavorId);

    List<TenantConfig> findByActiveTrue();
}
