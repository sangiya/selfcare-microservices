package com.selfcare.content.web;

import com.selfcare.content.domain.ContentArticle;
import com.selfcare.content.repository.ContentArticleRepository;
import com.selfcare.platform.common.tenant.TenantContext;
import com.selfcare.platform.common.web.ApiResponse;
import com.selfcare.platform.common.web.NotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/content")
@Validated
@Tag(name = "Help, Info & Content", description = "STARTER -- Doc 5 pilot domain 4 of 4; see README-TODO.md")
public class ContentController {

    private final ContentArticleRepository repository;

    public ContentController(ContentArticleRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/articles")
    public ApiResponse<List<ContentArticle>> listByCategory(
            @RequestParam @NotBlank String category, @RequestParam(defaultValue = "en") String language) {
        return ApiResponse.ok(repository.findByTenantIdAndCategoryAndLanguageAndPublishedTrueOrderByDisplayOrderAsc(
                TenantContext.get(), category, language));
    }

    @GetMapping("/articles/{id}")
    public ApiResponse<ContentArticle> getById(@PathVariable String id) {
        return ApiResponse.ok(repository.findById(id).orElseThrow(() -> new NotFoundException("No article with id=" + id)));
    }
}
