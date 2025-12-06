package com.bm.wschat.feature.wiki.controller;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.wiki.dto.request.CreateWikiArticleRequest;
import com.bm.wschat.feature.wiki.dto.request.UpdateWikiArticleRequest;
import com.bm.wschat.feature.wiki.dto.response.WikiArticleListResponse;
import com.bm.wschat.feature.wiki.dto.response.WikiArticleResponse;
import com.bm.wschat.feature.wiki.service.WikiArticleService;
import com.bm.wschat.shared.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wiki")
@RequiredArgsConstructor
public class WikiArticleController {

    private final WikiArticleService wikiArticleService;

    /**
     * Создать статью
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<WikiArticleResponse>> createArticle(
            @Valid @RequestBody CreateWikiArticleRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Article created",
                        wikiArticleService.createArticle(request, user.getId())));
    }

    /**
     * Список всех статей
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<WikiArticleListResponse>>> getAllArticles(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(wikiArticleService.getAllArticles(pageable)));
    }

    /**
     * Популярные статьи
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<Page<WikiArticleListResponse>>> getPopularArticles(
            @PageableDefault(size = 10, sort = "viewCount", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(wikiArticleService.getPopularArticles(pageable)));
    }

    /**
     * Поиск
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<WikiArticleListResponse>>> search(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(wikiArticleService.search(q, pageable)));
    }

    /**
     * По категории
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<WikiArticleListResponse>>> getByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(wikiArticleService.getByCategory(categoryId, pageable)));
    }

    /**
     * Получить по slug
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<WikiArticleResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(wikiArticleService.getBySlug(slug)));
    }

    /**
     * Обновить статью
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<WikiArticleResponse>> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWikiArticleRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Article updated",
                wikiArticleService.updateArticle(id, request, user.getId())));
    }

    /**
     * Удалить статью
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SPECIALIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        wikiArticleService.deleteArticle(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Article deleted"));
    }

    /**
     * Лайкнуть статью
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likeArticle(@PathVariable Long id) {
        wikiArticleService.likeArticle(id);
        return ResponseEntity.ok(ApiResponse.success("Article liked"));
    }
}
