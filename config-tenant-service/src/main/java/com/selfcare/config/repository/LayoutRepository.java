package com.selfcare.config.repository;

import com.selfcare.config.domain.LayoutDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LayoutRepository extends MongoRepository<LayoutDocument, String> {

    Optional<LayoutDocument> findByTenantIdAndScreenKey(String tenantId, String screenKey);
}
