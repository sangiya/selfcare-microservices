package com.selfcare.content.repository;

import com.selfcare.content.domain.ContentArticle;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ContentArticleRepository extends MongoRepository<ContentArticle, String> {

    List<ContentArticle> findByTenantIdAndCategoryAndLanguageAndPublishedTrueOrderByDisplayOrderAsc(
            String tenantId, String category, String language);
}
