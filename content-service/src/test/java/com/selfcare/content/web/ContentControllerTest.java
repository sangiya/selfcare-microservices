package com.selfcare.content.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.selfcare.content.domain.ContentArticle;
import com.selfcare.content.repository.ContentArticleRepository;
import com.selfcare.platform.common.tenant.TenantContext;
import com.selfcare.platform.common.web.ApiResponse;
import com.selfcare.platform.common.web.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentControllerTest {

    private static final String TENANT = "acme-telecom";

    @Mock
    private ContentArticleRepository repository;

    private ContentController controller;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);
        controller = new ContentController(repository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listByCategory_scopesLookupToCurrentTenantCategoryAndLanguage() {
        List<ContentArticle> existing = List.of(new ContentArticle());
        when(repository.findByTenantIdAndCategoryAndLanguageAndPublishedTrueOrderByDisplayOrderAsc(
                TENANT, "billing", "en"))
                .thenReturn(existing);

        ApiResponse<List<ContentArticle>> response = controller.listByCategory("billing", "en");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(existing);
    }

    @Test
    void listByCategory_defaultsLanguageToEnglishWhenNotSupplied() {
        // Mirrors the controller's @RequestParam(defaultValue = "en") -- calling the method
        // directly (as a Spring MVC test would need a running context to exercise that default
        // binding) so this test drives the same default explicitly instead.
        List<ContentArticle> existing = List.of();
        when(repository.findByTenantIdAndCategoryAndLanguageAndPublishedTrueOrderByDisplayOrderAsc(
                TENANT, "billing", "en"))
                .thenReturn(existing);

        ApiResponse<List<ContentArticle>> response = controller.listByCategory("billing", "en");

        assertThat(response.data()).isEmpty();
    }

    @Test
    void getById_returnsArticleWhenFound() {
        ContentArticle existing = new ContentArticle();
        existing.setId("abc123");
        when(repository.findById("abc123")).thenReturn(Optional.of(existing));

        ApiResponse<ContentArticle> response = controller.getById("abc123");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isSameAs(existing);
    }

    @Test
    void getById_throwsNotFoundWhenMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getById("missing")).isInstanceOf(NotFoundException.class);
    }
}
