package com.bm.wschat.shared.controller;

import com.bm.wschat.shared.common.ApiResponse;
import com.bm.wschat.shared.dto.CategoryResponse;
import com.bm.wschat.shared.dto.category.request.CreateCategoryRequest;
import com.bm.wschat.shared.dto.category.request.UpdateCategoryRequest;
import com.bm.wschat.shared.dto.category.response.CategoryDetailResponse;
import com.bm.wschat.shared.model.CategoryType;
import com.bm.wschat.shared.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Управление категориями тикетов и статей Wiki")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSADMIN','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Создать новую категорию", description = "Создает новую категорию для тикетов или статей Wiki.")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully",
                        categoryService.createCategory(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить категорию по ID", description = "Возвращает информацию о категории по ее уникальному идентификатору.")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id)));
    }

    @GetMapping
    @Operation(summary = "Получить список всех категорий", description = "Возвращает список всех доступных категорий.")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @GetMapping("/user-selectable")
    @Operation(summary = "Получить категории, доступные для выбора пользователем", description = "Возвращает список категорий, которые пользователи могут выбирать при создании тикетов.")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getUserSelectableCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getUserSelectableCategories()));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Получить категории по типу", description = "Возвращает список категорий указанного типа (например, GENERAL, HIDDEN).")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoriesByType(
            @PathVariable CategoryType type) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoriesByType(type)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSADMIN','DEV1C','DEVELOPER','ADMIN')")
    @Operation(summary = "Обновить категорию", description = "Обновляет существующую категорию.")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully",
                categoryService.updateCategory(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить категорию", description = "Удаляет категорию по ее уникальному идентификатору.")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully"));
    }
}
