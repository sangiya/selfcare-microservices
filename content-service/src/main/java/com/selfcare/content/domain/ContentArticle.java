package com.selfcare.content.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Consolidates the legacy Info/InfoCategories/InfoSubCategories/HelpCategories/
 * HelpSubCategories/Topic/Help controller family (README-TODO.md) into one document shape:
 * a category/subCategory-scoped article per language, per operator. TODO: confirm this shape
 * against the real content model in {@code VasInfoController.php} (2,692 LOC, the largest in
 * this domain) before treating it as final.
 */
@Document(collection = "content_article")
@CompoundIndex(name = "tenant_category_idx", def = "{'tenantId': 1, 'category': 1, 'language': 1}")
public class ContentArticle {

    @Id
    private String id;

    private String tenantId;
    private String category;
    private String subCategory;
    private String language;
    private String title;
    private String body;
    private boolean published = true;
    private int displayOrder;

    @LastModifiedDate
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
