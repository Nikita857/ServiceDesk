package com.bm.wschat.feature.wiki.controller;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.wiki.dto.request.CreateWikiArticleRequest;
import com.bm.wschat.feature.wiki.dto.request.UpdateWikiArticleRequest;
import com.bm.wschat.feature.wiki.dto.response.WikiArticleListResponse;
import com.bm.wschat.feature.wiki.dto.response.WikiArticleResponse;
import com.bm.wschat.feature.wiki.service.WikiArticleService;
import com.bm.wschat.feature.wiki.service.WikiDownloadService;
import com.bm.wschat.shared.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/wiki")
@RequiredArgsConstructor
@Tag(name = "Wiki Articles", description = "Управление статьями базы знаний")
public class WikiArticleController {

    private final WikiArticleService wikiArticleService;
    private final WikiDownloadService wikiDownloadService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSADMIN','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Создать новую статью Wiki", description = "Создает новую статью для базы знаний.")
    public ResponseEntity<ApiResponse<WikiArticleResponse>> createArticle(
            @Valid @RequestBody CreateWikiArticleRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Статья создана",
                        wikiArticleService.createArticle(request, user.getId())));
    }

    @GetMapping
    @Operation(summary = "Получить список всех статей Wiki", description = "Возвращает пагинированный список всех статей базы знаний.")
    public ResponseEntity<ApiResponse<Page<WikiArticleListResponse>>> getAllArticles(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(
                ApiResponse.success(
                        wikiArticleService.getAllArticles(pageable, userId)));
    }

    @GetMapping("/popular")
    @Operation(summary = "Получить список популярных статей Wiki", description = "Возвращает пагинированный список статей, отсортированных по количеству просмотров.")
    public ResponseEntity<ApiResponse<Page<WikiArticleListResponse>>> getPopularArticles(
            @PageableDefault(size = 10, sort = "viewCount", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(
                ApiResponse.success(
                        wikiArticleService.getPopularArticles(pageable, userId)));
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск статей Wiki", description = "Выполняет полнотекстовый поиск по статьям базы знаний.")
    public ResponseEntity<ApiResponse<Page<WikiArticleListResponse>>> search(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(
                ApiResponse.success(
                        wikiArticleService.search(q, pageable, userId)));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Получить статьи Wiki по категории", description = "Возвращает пагинированный список статей, принадлежащих к указанной категории.")
    public ResponseEntity<ApiResponse<Page<WikiArticleListResponse>>> getByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(
                ApiResponse.success(
                        wikiArticleService.getByCategory(categoryId, pageable, userId)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Получить статью Wiki по SLUG", description = "Возвращает статью базы знаний по ее уникальному SLUG.")
    public ResponseEntity<ApiResponse<WikiArticleResponse>> getBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        //Счетчик просмотров
        wikiArticleService.incrementViews(user, slug);
        return ResponseEntity.ok(
                ApiResponse.success(
                        wikiArticleService.getBySlug(slug, userId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSADMIN','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Обновить статью Wiki", description = "Обновляет существующую статью базы знаний.")
    public ResponseEntity<ApiResponse<WikiArticleResponse>> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWikiArticleRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.success("Статья обновлена",
                        wikiArticleService.updateArticle(id, request, user.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSADMIN','DEV1C','DEVELOPER')")
    @Operation(summary = "Удалить статью Wiki", description = "Удаляет статью базы знаний по ее ID (логическое удаление).")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        wikiArticleService.deleteArticle(id, user.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Статья удалена"));
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "Поставить лайк статье Wiki", description = "Увеличивает счетчик лайков для статьи базы знаний.")
    public ResponseEntity<ApiResponse<Void>> likeArticle(@PathVariable Long id,
                                                         @AuthenticationPrincipal User user) {
        wikiArticleService.likeArticle(id, user);
        return ResponseEntity.ok(
                ApiResponse.success("Статья лайкнута"));
    }

    @DeleteMapping("/{id}/like")
    @Operation(summary = "Убрать лайк со статьи Wiki", description = "Удаляет лайк пользователя со статьи базы знаний.")
    public ResponseEntity<ApiResponse<Void>> unlikeArticle(@PathVariable Long id,
                                                           @AuthenticationPrincipal User user) {
        wikiArticleService.unlikeArticle(id, user);
        return ResponseEntity.ok(
                ApiResponse.success("Лайк убран"));
    }

    @GetMapping("/{slug}/download")
    @Operation(summary = "Скачать PDF представление статьи", description = "Получает данные о статье из БД, создает PDF и отправляет его")
    public void downloadPdf(HttpServletResponse response, @PathVariable String slug) throws IOException {
        wikiDownloadService.generatePdf(response, slug);
    }
}
